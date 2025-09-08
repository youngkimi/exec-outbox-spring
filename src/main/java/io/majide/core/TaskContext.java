package io.majide.core;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class TaskContext {
    private final long runId;
    private final long instanceId;
    private final long taskId;
    private final String dagName;
    private final int dagVersion;
    private final String taskKey;
}