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
        var pickSql = """
            SELECT tr.run_id
              FROM TB_TASK_RUN tr
              JOIN TB_DAG_INSTANCE di ON di.instance_id = tr.instance_id
              JOIN TB_DAG_DEF dd ON dd.dag_id = di.dag_id
             WHERE tr.status = 'READY'
               AND tr.scheduled_at <= :now
               AND (tr.lease_expire_at IS NULL OR tr.lease_expire_at <= :now)
               AND di.status IN ('CREATED','RUNNING')
               AND dd.is_active = 'Y'
             FOR UPDATE SKIP LOCKED
             FETCH FIRST 1 ROWS ONLY
        """;
        var picked = tpl.query(pickSql, Map.of("now", now), rs -> rs.next()
                ? rs.getLong("run_id") : null);
        if (picked == null) return Optional.empty();

        long runId = picked;

        var updSql = """
            UPDATE TB_TASK_RUN t
               SET t.status = 'RUNNING',
                   t.lease_owner = :worker,
                   t.lease_expire_at = :leaseExpire,
                   t.started_at = COALESCE(t.started_at, :now)
             WHERE t.run_id = :runId
        """;

        var meta1 = tpl.queryForMap("""
            SELECT d.timeout_sec, t.task_id, t.instance_id
              FROM TB_TASK_RUN t
              JOIN TB_TASK_DEF d ON d.task_id = t.task_id
             WHERE t.run_id = :runId
        """, Map.of("runId", runId));

        int timeoutSec = ((Number) meta1.get("TIMEOUT_SEC")).intValue();
        long taskId = ((Number) meta1.get("TASK_ID")).longValue();
        long instanceId = ((Number) meta1.get("INSTANCE_ID")).longValue();

        tpl.update(updSql, new MapSqlParameterSource()
                .addValue("worker", workerId)
                .addValue("leaseExpire", now.plusSeconds(timeoutSec))
                .addValue("now", now)
                .addValue("runId", runId)
        );

        var metaSql = """
            SELECT d.handler_bean, d.handler_class, d.max_attempts, d.retry_backoff_ms
              FROM TB_TASK_DEF d
             WHERE d.task_id = :taskId
        """;
        var meta2 = tpl.queryForMap(metaSql, Map.of("taskId", taskId));

        return Optional.of(new Claim(
                runId, instanceId, taskId,
                (String) meta2.get("HANDLER_BEAN"),
                (String) meta2.get("HANDLER_CLASS"),
                timeoutSec,
                ((Number) meta2.get("MAX_ATTEMPTS")).intValue(),
                ((Number) meta2.get("RETRY_BACKOFF_MS")).intValue()
        ));
    }
}
