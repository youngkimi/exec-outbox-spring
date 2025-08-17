package io.youngkimi.springoutbox;

import io.youngkimi.springoutbox.service.DagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkerScheduler {

	private final DagService svc;

	@Value("${app.worker.enabled:true}")       private boolean enabled;
	@Value("${app.worker-id:local-worker}")    private String workerId;

	// READY/PROCESSING 재확보 (리스 만료) 주기적으로 복구
	@Scheduled(fixedDelayString = "${app.worker.requeue-interval-ms:5000}",
		initialDelayString = "${app.worker.initial-delay-ms:1000}")
	public void releaseExpired() {
		if (!enabled) return;
		int n = svc.releaseExpired();
		if (n > 0) log.debug("released {} expired events", n);
	}

	// 핵심 폴링 루프: 한 번에 하나만 claim (capacity=1은 DagService 쿼리에서 보장)
	@Scheduled(fixedDelayString = "${app.worker.poll-interval-ms:300}",
		initialDelayString = "${app.worker.initial-delay-ms:1000}")
	public void pollAndWork() {
		if (!enabled) return;

		// 같은 worker에서 동시 claim 방지는 DagService.claimOne() 내부의 pg_try_advisory_xact_lock로 보장
		Optional<DagService.Claimed> c = svc.claimOne();
		if (c.isEmpty()) {
			return; // 이번 틱엔 처리할 게 없음 (또는 이미 이 워커가 처리 중)
		}

		var ev = c.get();
		log.info("[{}] CLAIMED event={} inst={} task={}", workerId, ev.eventId(), ev.instanceId(), ev.taskKey());

		try {
			// ⚠️ 실제 헤비 잡 수행 위치
			// 예: doActualJob(ev);  // 멱등성 유지 필수
			Thread.sleep(300); // 데모용

			svc.completeSuccess(ev.eventId(), ev.instanceId());
			log.info("[{}] COMPLETED event={} task={}", workerId, ev.eventId(), ev.taskKey());
		} catch (Exception e) {
			log.error("job failed, event={}", ev.eventId(), e);
			// 필요 시 실패 처리(백오프/DLQ). DagService에 markFailed(...) 구현되어 있으면 호출
			// svc.markFailed(ev.eventId(), ev.instanceId(), "worker error: " + e.getMessage());
		}
	}
}

