package io.youngkimi.springoutbox.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
public class DagService {

	private final NamedParameterJdbcTemplate jdbc;
	private final String workerId;
	private final int capacity;
	private final int leaseSecs;

	public DagService(NamedParameterJdbcTemplate jdbc,
		@Value("${app.worker-id:local-1}") String workerId,
		@Value("${app.worker.capacity:1}") int capacity,
		@Value("${app.worker.lease-secs:300}") int leaseSecs) {
		this.jdbc = jdbc;
		this.workerId = workerId;
		this.capacity = capacity;
		this.leaseSecs = leaseSecs;
	}

	/** 워커가 점유한 이벤트 1건 */
	public record Claimed(Long eventId, Long runId, Long instanceId, String taskKey, OffsetDateTime visibleAt) {}

	// ------------------------------
	// 부트스트랩(스케줄러에서 호출)
	// ------------------------------
	/** 오늘자 bizKey/cutoff 계산 */
	public record BootstrapPlan(String bizKey, OffsetDateTime cutoffUtc) {}
	public BootstrapPlan planForToday(String prefix, ZoneId zone, LocalTime cutoff) {
		LocalDate today = LocalDate.now(zone);
		String bizKey = prefix + "_" + today;
		OffsetDateTime cutoffUtc = ZonedDateTime.of(today, cutoff, zone).toOffsetDateTime();
		return new BootstrapPlan(bizKey, cutoffUtc);
	}

	/** 멱등 부트스트랩(멀티노드 경쟁시 advisory-lock으로 1대만 성공) */
	@Transactional
	public Optional<Long> tryBootstrap(String bizKey, OffsetDateTime cutoffUtc) {
		Boolean locked = jdbc.queryForObject("""
      SELECT pg_try_advisory_xact_lock(
               (('x'||substr(md5(:key),1,16))::bit(64))::bigint
             )
      """, Map.of("key", "bootstrap:"+bizKey), Boolean.class);
		if (locked == null || !locked) return Optional.empty();

		Long ver = jdbc.queryForObject("""
        SELECT VERSION FROM TB_BAT_DAG_VERSION
         WHERE EFFECTIVE_AT <= :cut
         ORDER BY EFFECTIVE_AT DESC
         LIMIT 1
        """, Map.of("cut", cutoffUtc), Long.class);
		if (ver == null) throw new IllegalStateException("No DAG VERSION ≤ " + cutoffUtc);

		Long runId = jdbc.query("""
        INSERT INTO TB_BAT_DAG_RUN (DAG_VERSION, BIZ_KEY, STATUS)
        VALUES (:v, :biz, 'RUNNING')
        ON CONFLICT (DAG_VERSION, BIZ_KEY) DO NOTHING
        RETURNING RUN_ID
        """,
			new MapSqlParameterSource().addValue("v", ver).addValue("biz", bizKey),
			(rs, rn) -> rs.getLong(1)
		).stream().findFirst().orElseGet(() ->
			jdbc.queryForObject("""
          SELECT RUN_ID FROM TB_BAT_DAG_RUN WHERE DAG_VERSION=:v AND BIZ_KEY=:biz
        """, Map.of("v", ver, "biz", bizKey), Long.class)
		);

		// 인스턴스 생성 + 선행수 세팅
		jdbc.update("""
      WITH nodes AS (
        SELECT :v AS version, task_key FROM TB_BAT_TASK_DEF WHERE version=:v
      ),
      indeg AS (
        SELECT TO_KEY AS task_key, COUNT(*) AS indeg
        FROM TB_BAT_TASK_DEPENDENCY
        WHERE VERSION=:v GROUP BY TO_KEY
      )
      INSERT INTO TB_BAT_INSTANCE (RUN_ID, DAG_VERSION, BIZ_KEY, TASK_KEY, PRE_CNT, STATUS)
      SELECT :run, :v, :biz, n.task_key, COALESCE(i.indeg, 0), 'PENDING'
      FROM nodes n LEFT JOIN indeg i USING (task_key)
      ON CONFLICT (RUN_ID, TASK_KEY) DO NOTHING
      """, Map.of("v", ver, "run", runId, "biz", bizKey));

		// 초기 READY 큐잉
		enqueueNewlyReadyForRun(runId);
		return Optional.of(runId);
	}

	/** pre_cnt=0 & PENDING → READY 큐 삽입 (부트스트랩/전파에서 공통 사용) */
	@Transactional
	public int enqueueNewlyReadyForRun(long runId) {
		return jdbc.update("""
      WITH to_mark AS (
        UPDATE TB_BAT_INSTANCE i
           SET ENQUEUED_AT = now()
         WHERE i.RUN_ID=:run
           AND i.PRE_CNT=0 AND i.STATUS='PENDING'
           AND i.ENQUEUED_AT IS NULL
         RETURNING i.RUN_ID, i.ID, i.TASK_KEY
      )
      INSERT INTO TB_BAT_OUTBOX_EVENT (RUN_ID, INSTANCE_ID, TASK_KEY, PAYLOAD)
      SELECT RUN_ID, ID, TASK_KEY,
             jsonb_build_object('runId', RUN_ID, 'taskKey', TASK_KEY,
                                'idempotencyKey', concat(RUN_ID, ':', TASK_KEY))
        FROM to_mark
      ON CONFLICT DO NOTHING
      """, Map.of("run", runId));
	}

	// ------------------------------
	// 워커 루프에서 사용하는 3가지 (이번 컴파일 오류 원인)
	// ------------------------------

	/** 리스 만료된 PROCESSING을 READY로 복구 */
	@Transactional
	public int releaseExpired() {
		return jdbc.update("""
      UPDATE TB_BAT_OUTBOX_EVENT
         SET STATUS='READY', WORKER_ID=NULL, UPDATED_AT=now()
       WHERE STATUS='PROCESSING' AND VISIBLE_AT <= now()
      """, Map.of());
	}

	private static final RowMapper<Claimed> CLAIMED_MAPPER = (rs, rn) ->
		new Claimed(
			rs.getLong("id"),
			rs.getLong("run_id"),
			rs.getLong("instance_id"),
			rs.getString("task_key"),
			rs.getObject("visible_at", OffsetDateTime.class)
		);

	/** 원자적 claim(워커당 capacity 체크 + advisory-lock) */
	@Transactional
	public Optional<Claimed> claimOne() {
		// 같은 workerId의 동시 claim 직렬화(실패 시 이번 틱 skip)
		Boolean locked = jdbc.queryForObject("""
      SELECT pg_try_advisory_xact_lock(
               (('x'||substr(md5(:wid),1,16))::bit(64))::bigint
             )
      """, Map.of("wid", workerId), Boolean.class);
		if (locked == null || !locked) return Optional.empty();

		// inflight < capacity 조건으로 1건 픽
		List<Claimed> got = jdbc.query("""
      WITH inflight AS (
        SELECT COUNT(*) AS c
          FROM TB_BAT_OUTBOX_EVENT
         WHERE WORKER_ID=:wid AND STATUS='PROCESSING' AND VISIBLE_AT > now()
      ),
      pick AS (
        SELECT id
          FROM TB_BAT_OUTBOX_EVENT
         WHERE STATUS='READY' AND IS_DLQ=FALSE AND VISIBLE_AT <= now()
         ORDER BY id
         FOR UPDATE SKIP LOCKED
         LIMIT 1
      )
      UPDATE TB_BAT_OUTBOX_EVENT q
         SET STATUS='PROCESSING',
             WORKER_ID=:wid,
             VISIBLE_AT = now() + (:lease || ' seconds')::interval,
             UPDATED_AT=now()
       WHERE q.id IN (SELECT id FROM pick)
         AND (SELECT c FROM inflight) < :cap
      RETURNING q.*
      """,
			new MapSqlParameterSource()
				.addValue("wid", workerId)
				.addValue("lease", leaseSecs)
				.addValue("cap", capacity),
			CLAIMED_MAPPER
		);

		return got.stream().findFirst();
	}

	/** 성공 전파: 최초 COMPLETED만 인정 → 후행 PRE_CNT 감소 → 0이 된 것 큐잉 → 현재 이벤트 LOGGED */
	@Transactional
	public int completeSuccess(long eventId, long instanceId) {
		// 1) 완료 전파 → 후행 PRE_CNT ↓ → 0 된 PENDING을 ENQUEUED_AT 마킹 + 큐 INSERT
		jdbc.update("""
    WITH done AS (
      UPDATE TB_BAT_INSTANCE
         SET STATUS='COMPLETED', UPDATED_AT=now()
       WHERE ID=:instanceId AND STATUS IN ('PENDING','RUNNING')
       RETURNING RUN_ID, DAG_VERSION, TASK_KEY
    ),
    dec AS (
      UPDATE TB_BAT_INSTANCE d
         SET PRE_CNT = GREATEST(PRE_CNT - 1, 0), UPDATED_AT=now()
        FROM TB_BAT_TASK_DEPENDENCY dep, done
       WHERE dep.VERSION = done.DAG_VERSION
         AND dep.FROM_KEY = done.TASK_KEY
         AND d.RUN_ID = done.RUN_ID
         AND d.TASK_KEY = dep.TO_KEY
       RETURNING d.RUN_ID, d.ID AS instance_id, d.TASK_KEY, d.PRE_CNT, d.STATUS
    ),
    to_mark AS (
      UPDATE TB_BAT_INSTANCE i
         SET ENQUEUED_AT = now()
       WHERE i.ID IN (SELECT instance_id FROM dec WHERE PRE_CNT=0 AND STATUS='PENDING')
         AND i.ENQUEUED_AT IS NULL
       RETURNING i.RUN_ID, i.ID, i.TASK_KEY
    )
    INSERT INTO TB_BAT_OUTBOX_EVENT (RUN_ID, INSTANCE_ID, TASK_KEY, PAYLOAD)
    SELECT RUN_ID, ID, TASK_KEY,
           jsonb_build_object('runId', RUN_ID, 'taskKey', TASK_KEY,
                              'idempotencyKey', concat(RUN_ID, ':', TASK_KEY))
      FROM to_mark
    ON CONFLICT DO NOTHING
    """,
			new MapSqlParameterSource().addValue("instanceId", instanceId)
		);

		// 2) 현재 이벤트는 LOGGED로
		jdbc.update("""
    UPDATE TB_BAT_OUTBOX_EVENT
       SET STATUS='LOGGED',
           STAGED_AT=COALESCE(STAGED_AT, now()),
           LOGGED_AT=now(), UPDATED_AT=now()
     WHERE ID=:eventId AND STATUS='PROCESSING'
    """, new MapSqlParameterSource().addValue("eventId", eventId));

		// 3) ✅ 세이프티넷: 혹시라도 남아있는 0-프리건을 한 번 더 큐잉
		Long runId = jdbc.queryForObject(
			"SELECT RUN_ID FROM TB_BAT_INSTANCE WHERE ID=:iid",
			Map.of("iid", instanceId),
			Long.class
		);
		return enqueueNewlyReadyForRun(runId); // 중복 삽입은 ON CONFLICT로 무해
	}


	// (옵션) 모니터링용
	@Transactional(readOnly = true)
	public boolean isQueueDrained(long runId) {
		Integer c = jdbc.queryForObject(
			"SELECT COUNT(*) FROM TB_BAT_OUTBOX_EVENT WHERE RUN_ID=:run AND STATUS IN ('READY','PROCESSING')",
			Map.of("run", runId), Integer.class);
		return c == null || c == 0;
	}
}
