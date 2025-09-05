package io.majide.data.repository;

import io.majide.data.entity.TaskDef;
import io.majide.data.entity.DagDef;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository 
public interface TaskDefRepository extends JpaRepository<TaskDef, Long> {
    List<TaskDef> findByDagId(Long dagId);

    List<TaskDef> findByDag(DagDef dag);

    Optional<TaskDef> findByDagIdAndTaskKey(Long dagId, String taskKey);
}
