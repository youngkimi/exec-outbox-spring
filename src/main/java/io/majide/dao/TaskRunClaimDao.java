// com/example/orch/dao/TaskRunClaimDao.java
package io.majide.dao;

import java.time.Instant;
import java.util.Optional;

public interface TaskRunClaimDao {

    record Claim(
        long runId, long instanceId, long taskId,
        String handlerBean, String handlerClass,
        int timeoutSec, int maxAttempts, int retryBackoffMs
    ) {}

    Optional<Claim> claimNextReady(String workerId, Instant now);
}
