package io.majide.repository;

import io.majide.domain.DagInstance;
import io.majide.domain.DagInstanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository 
public interface DagInstanceRepository extends JpaRepository<DagInstance, Long> {
    Optional<DagInstance> findByDagIdAndPeriodKey(Long dagId, String periodKey);

    List<DagInstance> findByDagIdOrderByCreatedAtDesc(Long dagId);

    @Modifying
    @Query("""
        update DagInstance i
           set i.status = :to, i.startedAt = coalesce(i.startedAt, :now)
         where i.id = :id and i.status = :expected
        """)
    int updateStatusIf(Long id, DagInstanceStatus expected, DagInstanceStatus to, Instant now);

    @Modifying
    @Query("""
        update DagInstance i
           set i.status = :to, i.endedAt = :now
         where i.id = :id and i.status = :expected
        """)
    int finishIf(Long id, DagInstanceStatus expected, DagInstanceStatus to, Instant now);
}