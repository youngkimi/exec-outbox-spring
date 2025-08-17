// src/main/java/io/youngkimi/springoutbox/BootstrapScheduler.java
package io.youngkimi.springoutbox;

import io.youngkimi.springoutbox.service.DagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.*;

@Component
public class BootstrapScheduler {
	private static final Logger log = LoggerFactory.getLogger(BootstrapScheduler.class);

	private final DagService svc;
	public BootstrapScheduler(DagService svc) { this.svc = svc; }

	@Value("${app.bootstrap.enabled:true}")         boolean enabled;
	@Value("${app.bootstrap.bizkey-prefix:PNT}")    String prefix;
	@Value("${app.timezone:Asia/Seoul}")            String tz;
	@Value("${app.bootstrap.cutoff-time:06:00}")    String cutoffStr;

	// ✅ 앱 시작 직후 1회 즉시 시도 (개발/테스트 편의)
	@EventListener(ApplicationReadyEvent.class)
	public void bootstrapAtStartup() {
		if (!enabled) return;
		runOnce("startup");
	}

	// ✅ 매일 지정된 시간에 한번 더
	@Scheduled(cron = "${app.bootstrap.cron:0 5 6 * * *}", zone = "${app.timezone:Asia/Seoul}")
	public void daily() {
		if (!enabled) return;
		runOnce("daily-cron");
	}

	private void runOnce(String reason) {
		ZoneId zone = ZoneId.of(tz);
		LocalTime cutoff = LocalTime.parse(cutoffStr);
		var plan = svc.planForToday(prefix, zone, cutoff);
		var runId = svc.tryBootstrap(plan.bizKey(), plan.cutoffUtc());
		if (runId.isPresent()) {
			log.info("Bootstrap({}) OK: runId={} bizKey={}", reason, runId.get(), plan.bizKey());
			int q = svc.enqueueNewlyReadyForRun(runId.get()); // 안전망: 0인 것 한 번 더 큐잉
			log.info("enqueueNewlyReadyForRun added {} item(s)", q);
		} else {
			log.info("Bootstrap({}) skipped (another node handled it)", reason);
		}
	}
}
