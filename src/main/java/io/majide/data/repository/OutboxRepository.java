package io.youngkimi.springoutbox.data.entity;

import io.youngkimi.springoutbox.domain.OutboxStatus;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<Outbox, Long> {

    List<Outbox> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);

    @Modifying
    @Query("""
        update Outbox o
           set o.status = :to, o.publishedAt = :now, o.attempt = o.attempt + 1
         where o.id = :id and o.status = :expected
        """)
    int markPublishedIf(Long id, OutboxStatus expected, OutboxStatus to, Instant now);

    @Modifying
    @Query("""
        update Outbox o
           set o.status = io.majide.entity.OutboxStatus.FAILED, o.attempt = o.attempt + 1
         where o.id = :id and o.status = io.majide.entity.OutboxStatus.NEW
        """)
    int markPublishFailed(Long id);
}