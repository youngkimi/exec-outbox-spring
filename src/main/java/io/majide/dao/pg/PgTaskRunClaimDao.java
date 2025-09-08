// com/example/orch/dao/pg/PgTaskRunClaimDao.java
package io.majide.dao.pg;

import io.majide.dao.TaskRunClaimDao;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
@Profile("pg")
@RequiredArgsConstructor
public class PgTaskRunClaimDao implements TaskRunClaimDao {

    private final NamedParameterJdbcTemplate tpl;

    @Override
    public Optional<Claim> claimNextReady(String workerId, Instant now) {
        var sql = """
        WITH cte AS (
          SELECT tr.run_id, tr.task_id
            FROM tb_task_run tr
           WHERE tr.status = 'READY'
             AND tr.scheduled_at <= :now
             AND (tr.lease_expire_at IS NULL OR tr.lease_expire_at <= :now)
           ORDER BY tr.scheduled_at ASC
           FOR UPDATE SKIP LOCKED
           LIMIT 1
        ), upd AS (
          UPDATE tb_task_run t
             SET status = 'RUNNING',
                 lease_owner = :worker,
                 lease_expire_at = :leaseExpire,
                 started_at = COALESCE(t.started_at, :now)
            FROM cte
           WHERE t.run_id = cte.run_id
          RETURNING t.run_id, t.instance_id, t.task_id
        )
        SELECT u.run_id, u.instance_id, u.task_id,
               d.handler_bean, d.handler_class,
               d.timeout_sec, d.max_attempts, d.retry_backoff_ms
          FROM upd u
          JOIN tb_task_def d ON d.task_id = u.task_id
        """;

        var params = new MapSqlParameterSource()
                .addValue("now", now)
                .addValue("worker", workerId)
                .addValue("leaseExpire", now.plusSeconds(3600)); // 기본 1h, 곧바로 덮어씌움

        return tpl.query(sql, params, rs -> {
            if (!rs.next()) return Optional.empty();
            var timeout = rs.getInt("timeout_sec");
            // lease를 task timeout에 맞춰 다시 세팅(돌려쓰기 방지 위해 UPDATE 2회 피하려면 위에서 계산해 전달하는 구조로 가도 OK)
            // 간결하게 여기선 반환만 하고, 워커가 바로 heartbeat(now, now+timeout)로 정밀 세팅하도록 권장.
            return Optional.of(new Claim(
                    rs.getLong("run_id"),
                    rs.getLong("instance_id"),
                    rs.getLong("task_id"),
                    rs.getString("handler_bean"),
                    rs.getString("handler_class"),
                    timeout,
                    rs.getInt("max_attempts"),
                    rs.getInt("retry_backoff_ms")
            ));
        });
    }
}
