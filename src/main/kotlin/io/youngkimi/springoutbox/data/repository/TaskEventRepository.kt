package io.youngkimi.springoutbox.data.repository

import io.youngkimi.springoutbox.data.entity.TaskEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskEventRepository: JpaRepository<TaskEvent, Long> {
}