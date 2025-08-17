package io.youngkimi.springoutbox.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.youngkimi.springoutbox.data.entity.Instance;

public interface InstanceRepository extends JpaRepository<Instance, Long> {
}

