package io.youngkimi.springoutbox.data.entity

import jakarta.persistence.*

@Table(name = "TASK_DEPENDENCY")
@Entity
class TaskDependency(

    @EmbeddedId
    val id: TaskDependencyId

) {
    // JPA용 기본 생성자 (Kotlin no-arg 플러그인 없는 환경 대비)
    protected constructor() : this(TaskDependencyId(0L, "", ""))

    companion object {
        fun of(version: Long, fromKey: String, toKey: String) =
            TaskDependency(TaskDependencyId(version, fromKey, toKey))
    }
}
