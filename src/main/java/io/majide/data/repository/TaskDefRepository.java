package io.youngkimi.springoutbox.data.entity;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface TaskDefRepository extends JpaRepository<TaskDef, Long> {
    List<TaskDef> findByDagId(Long dagId);

    List<TaskDef> findByDag(DagDef dag);

    Optional<TaskDef> findByDagIdAndTaskKey(Long dagId, String taskKey);
}
