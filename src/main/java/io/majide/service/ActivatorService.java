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
        var instance = instRepo.findById(instanceId).orElseThrow();
        Long dagId = instance.getDag().getId();

        // completedTask -> toTask 후보들
        List<TaskDep> edges = depRepo.findAllDepsFromTask(dagId, completedTaskId);

        for (TaskDep edge : edges) {
            long toTaskId = edge.getToTask().getId();
            if (allPredecessorsSucceeded(instanceId, dagId, toTaskId)) {
                var existing = runRepo.findByInstanceIdAndTaskId(instanceId, toTaskId);
                if (existing.isEmpty()) {
                    var tr = TaskRun.builder()
                            .instance(instance)
                            .task(taskDefRepo.getReferenceById(toTaskId))
                            .status(TaskRunStatus.READY)
                            .attempt(0)
                            .scheduledAt(now)
                            .build();
                    runRepo.save(tr);
                }
            }
        }

        // 모든 태스크 성공 시 인스턴스 완료
        if (isAllTasksSucceeded(instanceId)) {
            instRepo.finishIf(instanceId, DagInstanceStatus.RUNNING, DagInstanceStatus.SUCCEEDED, now);
        }
    }

    private boolean allPredecessorsSucceeded(long instanceId, long dagId, long toTaskId) {
        List<TaskDep> preds = depRepo.findAllDepsOfToTask(dagId, toTaskId);
        for (TaskDep dep : preds) {
            var tr = runRepo.findByInstanceIdAndTaskId(instanceId, dep.getFromTask().getId());
            if (tr.isEmpty() || tr.get().getStatus() != TaskRunStatus.SUCCEEDED) {
                return false;
            }
        }
        return true;
    }

    private boolean isAllTasksSucceeded(long instanceId) {
        return runRepo.findByInstanceId(instanceId).stream()
                .allMatch(tr -> tr.getStatus() == TaskRunStatus.SUCCEEDED);
    }
}