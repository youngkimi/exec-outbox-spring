package io.majide.data.entity;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface ScheduleDefRepository extends JpaRepository<ScheduleDef, Long> {
    List<ScheduleDef> findByDagId(Long dagId);

    List<ScheduleDef> findByActiveAndNextFireAtLessThanEqual(Yn active, Instant byTime);
}
