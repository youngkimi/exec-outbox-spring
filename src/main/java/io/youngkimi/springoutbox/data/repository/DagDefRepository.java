package io.youngkimi.springoutbox.data.entity;


import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

import java.util.List;
import java.util.Optional;

public interface DagDefRepository extends JpaRepository<DagDef, Long> {
    Optional<DagDef> findByNameAndVersion(String name, Integer version);

    List<DagDef> findByNameOrderByVersionDesc(String name);

    List<DagDef> findByActive(Yn active);
}
