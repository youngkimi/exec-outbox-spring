// com/example/orch/service/ActivatorService.java
package io.majide.service;

import io.majide.domain.*;
import io.majide.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivatorService {

    private final TaskDepRepository depRepo;
    private final TaskRunRepository runRepo;
    private final TaskDefRepository taskDefRepo;
    private final DagInstanceRepository instRepo;

    @Transactional
    public void onTaskSucceeded(long instanceId, long completedTaskId, Instant now) {
        // 1) completedTask를 선행으로 갖는 모든 후행 toTask
        List<TaskDep> edges = depRepo.findAllDepsFromTask(
                getInstance(instanceId).getDag().getId(), completedTaskId);

        for (TaskDep edge : edges) {
            long toTaskId = edge.getToTask().getId();
            if (allPredecessorsSucceeded(instanceId, toTaskId)) {
                // READY 만들기 or 이미 존재하면 상태만 조정
                var existing = runRepo.findByInstanceIdAndTaskId(instanceId, toTaskId);
                if (existing.isEmpty()) {
                    var tr = TaskRun.builder()
                            .instance(getInstance(instanceId))
                            .task(taskDefRepo.getReferenceById(toTaskId))
                            .status(TaskRunStatus.READY)
                            .attempt(0)
                            .scheduledAt(now)        // Instant
                            .build();
                    runRepo.save(tr);
                } else if (existing.get().getStatus() == TaskRunStatus.RETRY_WAIT) {
                    // 정책에 따라 READY로 전환할 수도 있으나, 여기선 그대로 두고 스케줄에 맡김
                }
            }
        }

        // 모든 태스크가 SUCCEEDED면 인스턴스 완료
        if (isAllTasksSucceeded(instanceId)) {
            instRepo.finishIf(instanceId, DagInstanceStatus.RUNNING, DagInstanceStatus.SUCCEEDED, now);
        }
    }

    private boolean allPredecessorsSucceeded(long instanceId, long toTaskId) {
        // toTask의 모든 선행 fromTask가 SUCCEEDED 인지 확인
        List<TaskDep> preds = depRepo.findAllDepsOfToTask(getInstance(instanceId).getDag().getId(), toTaskId);
        for (TaskDep dep : preds) {
            var tr = runRepo.findByInstanceIdAndTaskId(instanceId, dep.getFromTask().getId());
            if (tr.isEmpty() || tr.get().getStatus() != TaskRunStatus.SUCCEEDED) {
                return false;
            }
        }
        return true;
    }

    private boolean isAllTasksSucceeded(long instanceId) {
        // 인스턴스에 존재하는 모든 task_run이 SUCCEEDED인지
        return runRepo.findByInstanceId(instanceId).stream()
                .allMatch(tr -> tr.getStatus() == TaskRunStatus.SUCCEEDED);
    }

    private DagInstance getInstance(long id) {
        return instRepo.findById(id).orElseThrow();
    }
}
