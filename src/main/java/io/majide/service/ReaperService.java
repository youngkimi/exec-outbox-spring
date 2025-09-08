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

    // 간단 버전: 만료된 RUNNING → TIMED_OUT 후 즉시 재시도 대기
    @Scheduled(fixedDelayString = "${orch.reaper.scan-ms:2000}")
    public void reap() {
        Instant now = Instant.now(clock);
        // 네이티브/JPQL로 만료 건만 찾아서 전환하는 메서드를 별도로 두어도 좋다.
        runRepo.findByStatusAndLeaseExpireAtBefore(TaskRunStatus.RUNNING, now)
               .forEach(tr -> {
                   runRepo.complete(tr.getId(), TaskRunStatus.TIMED_OUT, now);
                   runRepo.scheduleRetry(tr.getId(), now.plusSeconds(5), now); // 간단 백오프 5s
               });
    }
}
