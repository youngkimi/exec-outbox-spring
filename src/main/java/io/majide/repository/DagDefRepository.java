package io.majide.repository;

import io.majide.domain.DagDef;
import io.majide.domain.Yn;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.Optional;

@Repository
public interface DagDefRepository extends JpaRepository<DagDef, Long> {
    Optional<DagDef> findByNameAndVersion(String name, Integer version);

    List<DagDef> findByNameOrderByVersionDesc(String name);

    List<DagDef> findByActive(Yn active);
}

