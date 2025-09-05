package io.youngkimi.springoutbox.data.entity;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyRepository extends JpaRepository<Idempotency, Long> {

    @Query("""
        select i from Idempotency i
         where i.dag.id = :dagId
           and ((:taskId is null and i.task is null) or (i.task.id = :taskId))
           and i.scope = :scope
           and i.key = :key
        """)
    Optional<Idempotency> findKey(Long dagId, Long taskId, String scope, String key);

    boolean existsByDagIdAndScopeAndKey(Long dagId, String scope, String key);
}
