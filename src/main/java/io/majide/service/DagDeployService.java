@Service
@RequiredArgsConstructor
public class DagDeployService {

  private final DagDefRepository dagRepo;
  private final TaskDefRepository taskDefRepo;
  private final TaskDepRepository depRepo;
  private final TaskResolver resolver; // handler 검증용
  private final Clock clock;

  @Transactional
  public long deploy(DagSpec spec) {
    // 1) DAG 헤더(비활성)
    var dag = DagDef.builder()
        .name(spec.name())
        .version(spec.version())
        .active(Yn.N)
        .createdAt(Instant.now(clock).atOffset(ZoneOffset.UTC))
        .build();
    dag = dagRepo.save(dag);

    // 2) Task 등록
    Map<String, TaskDef> byKey = new HashMap<>();
    for (var t : spec.tasks()) {
      var td = TaskDef.builder()
          .dag(dag).taskKey(t.key()).taskName(t.name())
          .handlerBean(t.handlerBean()).handlerClass(t.handlerClass())
          .timeoutSec(t.timeoutSec()).maxAttempts(t.maxAttempts())
          .retryBackoffMs(t.retryBackoffMs()).priority(t.priority())
          .createdAt(Instant.now(clock).atOffset(ZoneOffset.UTC))
          .build();
      td = taskDefRepo.save(td);
      byKey.put(t.key(), td);
    }

    // 3) DEP 등록
    for (var e : spec.edges()) {
      var from = byKey.get(e.from());
      var to   = byKey.get(e.to());
      if (from == null || to == null) throw new IllegalArgumentException("edge references unknown task");
      depRepo.save(TaskDep.builder().dag(dag).fromTask(from).toTask(to).optional(Yn.N).build());
    }

    // 4) 검증
    validateHandlers(byKey.values());   // 빈/클래스 로딩 가능?
    validateAcyclic(byKey.values(), depRepo, dag.getId()); // 사이클 없음?
    validateRoots(byKey.values(), depRepo, dag.getId());   // 루트 존재?

    // 5) 활성화
    dag.setActive(Yn.Y);
    dag.setUpdatedAt(Instant.now(clock).atOffset(ZoneOffset.UTC));
    // JPA flush는 트랜잭션 종료 시
    return dag.getId();
  }

  private void validateHandlers(Collection<TaskDef> defs) {
    for (TaskDef d : defs) {
      resolver.resolve(d); // 못 찾으면 예외 → 롤백
    }
  }
}
