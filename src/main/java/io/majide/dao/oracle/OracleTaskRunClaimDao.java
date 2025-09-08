// com/example/orch/dao/oracle/OracleTaskRunClaimDao.java
package io.majide.dao.oracle;

import io.majide.dao.TaskRunClaimDao;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Repository
@Profile("oracle")
@RequiredArgsConstructor
public class OracleTaskRunClaimDao implements TaskRunClaimDao {

    private final NamedParameterJdbcTemplate tpl;

    @Override
    public Optional<Claim> claimNextReady(String workerId, Instant now) {
        // 1) 후보 하나 잠그기
        var pickSql = """
            SELECT run_id, task_id
              FROM TB_TASK_RUN
             WHERE status = 'READY'
               AND scheduled_at <= :now
               AND (lease_expire_at IS NULL OR lease_expire_at <= :now)
             FOR UPDATE SKIP LOCKED
             FETCH FIRST 1 ROWS ONLY
        """;
        var picked = tpl.query(pickSql, Map.of("now", now), rs -> rs.next()
                ? Map.of("runId", rs.getLong("run_id"), "taskId", rs.getLong("task_id"))
                : null
        );
        if (picked == null) return Optional.empty();

        long runId = (long) picked.get("runId");
        long taskId = (long) picked.get("taskId");

        // 2) RUNNING 전이
        var updSql = """
            UPDATE TB_TASK_RUN
               SET status = 'RUNNING',
                   lease_owner = :worker,
                   lease_expire_at = :leaseExpire,
                   started_at = COALESCE(started_at, :now)
             WHERE run_id = :runId
        """;
        tpl.update(updSql, new MapSqlParameterSource()
                .addValue("worker", workerId)
                .addValue("leaseExpire", now.plusSeconds(3600)) // 임시(워커에서 heartbeat로 정확히 설정)
                .addValue("now", now)
                .addValue("runId", runId)
        );

        // 3) 실행 메타 조회
        var metaSql = """
            SELECT tr.run_id, tr.instance_id, tr.task_id,
                   d.handler_bean, d.handler_class,
                   d.timeout_sec, d.max_attempts, d.retry_backoff_ms
              FROM TB_TASK_RUN tr
              JOIN TB_TASK_DEF d ON d.task_id = tr.task_id
             WHERE tr.run_id = :runId
        """;
        return tpl.query(metaSql, Map.of("runId", runId), rs -> {
            if (!rs.next()) return Optional.empty();
            return Optional.of(new Claim(
                    rs.getLong("run_id"),
                    rs.getLong("instance_id"),
                    rs.getLong("task_id"),
                    rs.getString("handler_bean"),
                    rs.getString("handler_class"),
                    rs.getInt("timeout_sec"),
                    rs.getInt("max_attempts"),
                    rs.getInt("retry_backoff_ms")
            ));
        });
    }
}
