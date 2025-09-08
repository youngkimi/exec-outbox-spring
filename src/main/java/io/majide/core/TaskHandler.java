package io.majide.core;

public interface TaskHandler<I, O> {
    TaskResult<O> run(TaskContext ctx, I in, TaskControl ctl) throws Exception;
}