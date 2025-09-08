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

    private final Clock clock; // @Bean Clock.systemUTC()
    private final TaskRunClaimDao claimDao;
    private final TaskResolver resolver;
    private final TaskRunRepository runRepo;
    private final TaskDefRepository taskDefRepo;
    private final DagInstanceRepository instRepo;
    private final ActivatorService activator;
    private final ObjectMapper om;

    private final String workerId = java.util.UUID.randomUUID().toString(); // 간단 워커 식별자

    // 인스턴스당 하나만 수행: 메서드 자체가 단일 슬롯으로만 동작 (@Scheduled 단일 쓰레드)
    @Scheduled(fixedDelayString = "${orch.worker.poll-ms:500}")
    public void tick() {
        Instant now = Instant.now(clock);

        Optional<TaskRunClaimDao.Claim> claim = claimDao.claimNextReady(workerId, now);
        if (claim.isEmpty()) return; // 할 일 없음

        var c = claim.get();

        // 정확한 lease 만료를 task timeout 기반으로 갱신
        heartbeat(c.runId(), now, now.plusSeconds(c.timeoutSec()));

        // 실행기 해석
        var taskDef = taskDefRepo.getReferenceById(c.taskId());
        TaskHandler<?,?> handler = resolver.resolve(taskDef);

        // 컨텍스트/컨트롤 준비
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

        // 입력 로드
        var run = runRepo.findById(c.runId()).orElseThrow();
        Map<String,Object> in = readJson(run.getPayloadInJson());

        try {
            // 비즈니스 실행
            @SuppressWarnings("unchecked")
            TaskResult<Map<String,Object>> result =
                    (TaskResult<Map<String,Object>>) handler.run(ctx, in, ctl);

            // 성공 처리
            onSuccess(c.runId(), result == null ? null : result.getOutput(), now);

            // 후행 활성화
            activator.onTaskSucceeded(c.instanceId(), c.taskId(), now);

        } catch (Exception ex) {
            log.warn("Task failed runId={} : {}", c.runId(), ex.toString(), ex);
            onFailure(c, ex, now);
        }
    }

    @Transactional
    void heartbeat(long runId, Instant hbNow, Instant leaseExpire) {
        runRepo.heartbeat(runId, hbNow, leaseExpire);
    }

    @Transactional
    void onSuccess(long runId, Map<String,Object> output, Instant now) {
        var run = runRepo.findById(runId).orElseThrow();
        run.setPayloadOutJson(writeJson(output));
        runRepo.complete(runId, TaskRunStatus.SUCCEEDED, now);
    }

    @Transactional
    void onFailure(TaskRunClaimDao.Claim c, Exception ex, Instant now) {
        var run = runRepo.findById(c.runId()).orElseThrow();
        run.setErrorCode(ex.getClass().getSimpleName());
        run.setErrorMsg(shorten(ex.getMessage(), 2000));

        int attempts = Optional.ofNullable(run.getAttempt()).orElse(0);
        if (attempts + 1 < c.maxAttempts()) {
            // 재시도
            Instant next = now.plusMillis(c.retryBackoffMs());
            runRepo.scheduleRetry(run.getId(), next, now);
        } else {
            // 최종 실패
            runRepo.complete(run.getId(), TaskRunStatus.FAILED, now);
            // 인스턴스 실패 전파는 정책에 따라… (여기선 Activator가 후행을 열지 않으므로 정지)
        }
    }

    private Map<String,Object> readJson(String json) {
        try {
            return json == null ? Map.of() : om.readValue(json, om.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
        } catch (Exception e) {
            throw new IllegalStateException("payload_in JSON parse error", e);
        }
    }
    private String writeJson(Map<String,Object> map) {
        try { return map == null ? null : om.writeValueAsString(map); }
        catch (Exception e) { throw new IllegalStateException("payload_out JSON write error", e); }
    }
    private String shorten(String s, int max) { return s == null ? null : (s.length() <= max ? s : s.substring(0, max)); }
}
