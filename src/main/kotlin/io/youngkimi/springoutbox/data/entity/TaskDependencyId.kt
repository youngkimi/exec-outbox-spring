package io.youngkimi.springoutbox.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class TaskDependencyId(
    @Column(name = "VERSION", nullable = false, precision = 10, scale = 0)
    val version: Long,

    @Column(name = "FROM_KEY", nullable = false, length = 200)
    val fromKey: String,

    @Column(name = "TO_KEY", nullable = false, length = 200)
    val toKey: String
) : Serializable