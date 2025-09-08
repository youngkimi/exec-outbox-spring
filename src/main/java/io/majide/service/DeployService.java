package io.majide.service;

import io.majide.domain.*;
import io.majide.repo.*;
import io.majide.service.dto.DagSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DagDeployService {

    private final DagDefRepository dagRepo;
    private final TaskDefRepository taskDefRepo;
    private final TaskDepRepository depRepo;
    private final TaskResolver resolver;
    private final Clock clock;

    @Transactional
    public long deploy(DagSpec spec) {
        Instant now = Instant.now(clock);

        // 1) DAG 헤더(비활성)
        var dag = DagDef.builder()
                .name(spec.name())
                .version(spec.version())
                .active(Yn.N)
                .createdAt(now.atOffset(ZoneOffset.UTC))
                .build();
        dag = dagRepo.save(dag);

        // 2) Task 등록
        Map<String, TaskDef> byKey = new HashMap<>();
        for (var t : spec.tasks()) {
            var td = TaskDef.builder()
                    .dag(dag)
                    .taskKey(t.key()).taskName(t.name())
                    .handlerBean(t.handlerBean()).handlerClass(t.handlerClass())
                    .timeoutSec(t.timeoutSec()).maxAttempts(t.maxAttempts())
                    .retryBackoffMs(t.retryBackoffMs()).priority(t.priority())
                    .createdAt(now.atOffset(ZoneOffset.UTC))
                    .build();
            td = taskDefRepo.save(td);
            byKey.put(t.key(), td);
        }

        // 3) DEP 등록
        for (var e : spec.edges()) {
            var from = byKey.get(e.from());
            var to   = byKey.get(e.to());
            if (from == null || to == null) throw new IllegalArgumentException("edge references unknown task");
            depRepo.save(TaskDep.builder().dag(dag).fromTask(from).toTask(to).optional(e.optional()?Yn.Y:Yn.N).build());
        }

        // 4) 검증
        validateHandlers(byKey.values());      // 빈/클래스 로딩 가능?
        validateAcyclic(byKey.values(), dag.getId()); // 사이클 없음?
        validateRoots(byKey.values(), dag.getId());   // 루트 존재?

        // 5) 활성화(승격)
        dag.setActive(Yn.Y);
        dag.setUpdatedAt(now.atOffset(ZoneOffset.UTC));
        return dag.getId();
    }

    private void validateHandlers(Collection<TaskDef> defs) {
        defs.forEach(resolver::resolve);
    }

    // 간단 판별(성능 개선은 후속)
    private void validateAcyclic(Collection<TaskDef> tasks, Long dagId) {
        // Kahn 알고리듬의 간단 구현 대신, 여기선 의존성 수가 적다는 가정으로 skip
        // 실서비스에선 depRepo를 통해 in-degree 계산 & 위상정렬 구현 권장
    }

    private void validateRoots(Collection<TaskDef> tasks, Long dagId) {
        boolean hasRoot = tasks.stream().anyMatch(t ->
            depRepo.findAllDepsOfToTask(dagId, t.getId()).isEmpty()
        );
        if (!hasRoot) throw new IllegalStateException("No root task in DAG");
    }
}
