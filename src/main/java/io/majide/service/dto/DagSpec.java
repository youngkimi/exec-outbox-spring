package io.majide.service.dto;

import java.util.List;

public record DagSpec(
        String name,
        int version,
        List<TaskSpec> tasks,
        List<EdgeSpec> edges
) {
    public record TaskSpec(String key, String name, String handlerBean, String handlerClass,
                           int timeoutSec, int maxAttempts, int retryBackoffMs, int priority) {}
    public record EdgeSpec(String from, String to, boolean optional) {}
}