package io.majide.core;

import lombok.*;

@Getter @AllArgsConstructor(staticName = "succeeded")
public class TaskResult<O> {
    private final O output;
    public static <O> TaskResult<O> of(O output) { return new TaskResult<>(output); }
}