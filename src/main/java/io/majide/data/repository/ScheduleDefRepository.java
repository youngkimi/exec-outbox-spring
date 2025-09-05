package io.majide.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.Instant;

@Repository 
public interface ScheduleDefRepository extends JpaRepository<ScheduleDef, Long> {
    List<ScheduleDef> findByDagId(Long dagId);

    List<ScheduleDef> findByActiveAndNextFireAtLessThanEqual(Yn active, Instant byTime);
}
