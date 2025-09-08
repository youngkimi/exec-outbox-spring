package io.majide.repository;

import io.majide.domain.TaskDep;
import io.majide.domain.TaskDef;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository 
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
