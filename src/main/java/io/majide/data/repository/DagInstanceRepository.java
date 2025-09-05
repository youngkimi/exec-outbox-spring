package io.youngkimi.springoutbox.data.entity;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface DagInstanceRepository extends JpaRepository<DagInstance, Long> {
    Optional<DagInstance> findByDagIdAndPeriodKey(Long dagId, String periodKey);

    List<DagInstance> findByDagIdOrderByCreatedAtDesc(Long dagId);

    // 상태 전이(낙관적 제어): 기대 상태일 때만 변경
    @Modifying
    @Query("""
        update DagInstance i
           set i.status = :to, i.startedAt = coalesce(i.startedAt, current timestamp)
         where i.id = :id and i.status = :expected
        """)
    int updateStatusIf(Long id, DagInstanceStatus expected, DagInstanceStatus to);

    @Modifying
    @Query("""
        update DagInstance i
           set i.status = :to, i.endedAt = current timestamp
         where i.id = :id and i.status = :expected
        """)
    int finishIf(Long id, DagInstanceStatus expected, DagInstanceStatus to);
}