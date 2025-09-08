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
          SELECT tr.run_id
            FROM tb_task_run tr
            JOIN tb_dag_instance di ON di.instance_id = tr.instance_id
            JOIN tb_dag_def dd ON dd.dag_id = di.dag_id
           WHERE tr.status = 'READY'
             AND tr.scheduled_at <= :now
             AND (tr.lease_expire_at IS NULL OR tr.lease_expire_at <= :now)
             AND di.status IN ('CREATED','RUNNING')
             AND dd.is_active = true
           ORDER BY tr.scheduled_at ASC
           FOR UPDATE SKIP LOCKED
           LIMIT 1
        ), upd AS (
          UPDATE tb_task_run t
             SET status = 'RUNNING',
                 lease_owner = :worker,
                 lease_expire_at = :now + make_interval(secs => d.timeout_sec),
                 started_at = COALESCE(t.started_at, :now)
            FROM cte
            JOIN tb_task_def d ON d.task_id = t.task_id
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
                .addValue("worker", workerId);

        return tpl.query(sql, params, rs -> {
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