package io.youngkimi.springoutbox.data.entity;

public enum TaskRunStatus {
    READY, RUNNING, RETRY_WAIT, SUCCEEDED, FAILED, TIMED_OUT, CANCELLED
}