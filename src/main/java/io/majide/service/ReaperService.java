// com/example/orch/service/ReaperService.java
package io.majide.service;

import io.majide.domain.TaskRunStatus;
import io.majide.repo.TaskRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ReaperService {
    private final TaskRunRepository runRepo;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${orch.reaper.scan-ms:2000}")
    public void reap() {
        Instant now = Instant.now(clock);

        var batch = runRepo.findByStatusAndLeaseExpireAtBeforeOrderByLeaseExpireAtAsc(
                TaskRunStatus.RUNNING, now, PageRequest.of(0, 200));

        for (var tr : batch) {
            runRepo.complete(tr.getId(), TaskRunStatus.TIMED_OUT, now);
            runRepo.scheduleRetry(tr.getId(), now.plusSeconds(5), now); // 간단 백오프
        }
    }
}
