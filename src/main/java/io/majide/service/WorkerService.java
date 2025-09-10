package io.majide.service;

import io.majide.core.*;
import io.majide.dao.TaskRunClaimDao;
import io.majide.domain.*;
import io.majide.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerService {

    private final Clock clock;                         // UTC 고정 Clock 주입 (OrchConfig에서 제공)
    private final TaskRunClaimDao claimDao;            // DB 선점 DAO (PG/Oracle 프로파일 분기)
    private final TaskResolver resolver;               // 핸들러 해석기(빈 우선, 없으면 클래스)
    private final TaskRunRepository runRepo;           // 상태 전이/하트비트
    private final TaskDefRepository taskDefRepo;       // 메타 조회
    private final DagInstanceRepository instRepo;      // 컨텍스트 구성
    private final ActivatorService activator;          // 후행 활성화
    private final ObjectMapper om;                     // JSON 직렬화/역직렬화

    private final String workerId = UUID.randomUUID().toString(); // 간단 워커 ID

    /**
     * 인스턴스당 하나의 Task만 수행: 이 @Scheduled 메서드는 단일 슬롯으로만 동작.
     */
    @Scheduled(fixedDelayString = "${orch.worker.poll-ms:500}")
    public void tick() {
        Instant now = Instant.now(clock);

        var claimOpt = claimDao.claimNextReady(workerId, now);
        if (claimOpt.isEmpty()) return;

        var c = claimOpt.get();

        // 실행 메타
        var taskDef  = taskDefRepo.getReferenceById(c.taskId());
        var instance = instRepo.getReferenceById(c.instanceId());

        TaskContext ctx = TaskContext.builder()
                .runId(c.runId())
                .instanceId(c.instanceId())
                .taskId(c.taskId())
                .dagName(instance.getDagNameSnapshot())
                .dagVersion(instance.getDagVersion())
                .taskKey(taskDef.getTaskKey())
                .build();

        TaskControl ctl = (hbNow, newLease) -> heartbeat(c.runId(), hbNow, newLease);

        // 입력 로드(JSON → Map)
        var run = runRepo.findById(c.runId()).orElseThrow();
        Map<String, Object> in = readJson(run.getPayloadInJson());

        try {
            // ===== 핸들러 해석 & 실행 =====
            // resolver가 TaskHandler<?,?> 를 반환하는 환경도 고려하여, 여기서 브리지 캐스팅 처리
            Object resolved = resolver.resolve(taskDef);

            @SuppressWarnings({ "rawtypes", "unchecked" })
            TaskHandler<Map<String, Object>, Map<String, Object>> handler =
                    (TaskHandler) resolved;

            TaskResult<Map<String, Object>> result = handler.run(ctx, in, ctl);

            // ===== 성공 처리 =====
            onSuccess(c.runId(), result == null ? null : result.getOutput(), now);

            // 후행 활성화
            activator.onTaskSucceeded(c.instanceId(), c.taskId(), now);

        } catch (Exception ex) {
            log.warn("Task failed runId={} : {}", c.runId(), ex.toString(), ex);
            onFailure(c, ex, now);
        }
    }

    /**
     * 워커가 주기적으로 호출하여 임대를 연장(heartbeat)한다.
     */
    @Transactional
    void heartbeat(long runId, Instant hbNow, Instant leaseExpire) {
        runRepo.heartbeat(runId, hbNow, leaseExpire);
    }

    /**
     * 성공 처리: 출력 페이로드 저장 후 SUCCEEDED 전이.
     */
    @Transactional
    void onSuccess(long runId, Map<String, Object> output, Instant now) {
        var run = runRepo.findById(runId).orElseThrow();
        run.setPayloadOutJson(writeJson(output));
        runRepo.complete(runId, TaskRunStatus.SUCCEEDED, now);
    }

    /**
     * 실패 처리: 재시도 가능하면 RETRY_WAIT로 스케줄, 아니면 FAILED.
     */
    @Transactional
    void onFailure(TaskRunClaimDao.Claim c, Exception ex, Instant now) {
        var run = runRepo.findById(c.runId()).orElseThrow();
        run.setErrorCode(ex.getClass().getSimpleName());
        run.setErrorMsg(shorten(ex.getMessage(), 2000));

        int attempts = Optional.ofNullable(run.getAttempt()).orElse(0);
        if (attempts + 1 < c.maxAttempts()) {
            Instant next = now.plusMillis(c.retryBackoffMs());
            runRepo.scheduleRetry(run.getId(), next, now);
        } else {
            runRepo.complete(run.getId(), TaskRunStatus.FAILED, now);
        }
    }

    // ===== 유틸 =====

    private Map<String, Object> readJson(String json) {
        try {
            if (json == null || json.isBlank()) return Map.of();
            var type = om.getTypeFactory().constructMapType(Map.class, String.class, Object.class);
            return om.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("payload_in JSON parse error", e);
        }
    }

    private String writeJson(Map<String, Object> map) {
        try {
            return map == null ? null : om.writeValueAsString(map);
        } catch (Exception e) {
            throw new IllegalStateException("payload_out JSON write error", e);
        }
    }

    private String shorten(String s, int max) {
        if (s == null) return null;
        return (s.length() <= max) ? s : s.substring(0, max);
    }
}