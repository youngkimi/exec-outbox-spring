package io.youngkimi.springoutbox.data.repository

import io.youngkimi.springoutbox.data.entity.TaskInstance
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskInstanceRepository: JpaRepository<TaskInstance, Long> {
}