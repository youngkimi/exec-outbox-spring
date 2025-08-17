package io.youngkimi.springoutbox.data.repository

import io.youngkimi.springoutbox.data.entity.TaskDependency
import io.youngkimi.springoutbox.data.entity.TaskDependencyId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskDepRepository: JpaRepository<TaskDependency, TaskDependencyId> {
}