package io.youngkimi.springoutbox.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import io.youngkimi.springoutbox.data.entity.DagRun;

public interface DagRunRepository extends JpaRepository<DagRun, Long> {
	Optional<DagRun> findByDagVersionAndBizKey(Long dagVersion, String bizKey);
}

