package io.majide.data.entity;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface TaskDepRepository extends JpaRepository<TaskDep, Long> {

    // TODO - 특정 Task의 모든 TaskDep 

    // 특정 toTask의 모든 선행 fromTask들
    List<TaskDep> findByToTask(TaskDef toTask);

    // 특정 DAG 내에서 (toTask 기준)
    @Query("select d from TaskDep d where d.dag.id = :dagId and d.toTask.id = :toTaskId")
    List<TaskDep> findAllDepsOfToTask(Long dagId, Long toTaskId);

    // 특정 DAG 내에서 (fromTask 기준)
    @Query("select d from TaskDep d where d.dag.id = :dagId and d.fromTask.id = :fromTaskId")
    List<TaskDep> findAllDepsFromTask(Long dagId, Long fromTaskId);
}
