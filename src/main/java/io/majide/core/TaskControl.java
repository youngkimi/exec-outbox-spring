package io.majide.core;

import java.time.Instant;

public interface TaskControl {
    void heartbeat(Instant now, Instant newLeaseExpire);
}