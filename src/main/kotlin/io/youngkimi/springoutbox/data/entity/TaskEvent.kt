package io.youngkimi.springoutbox.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.time.OffsetDateTime

@Table(name = "TASK_INSTANCE_EVENT")
@Entity
class TaskEvent(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    val id: Long? = null,

    @Column(name = "INSTANCE_ID", nullable = false)
    val instanceId: Long,

    @Column(name = "TASK_KEY", nullable = false, length = 200)
    val taskKey: String,

    @Column(name = "STATUS", nullable = false, length = 16)
    val status: String = "READY",

    @Column(name = "WORKER_ID", length = 128)
    val workerId: String? = null,

    @Column(name = "VISIBLE_AT", nullable = false)
    val visibleAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "ATTEMPTS", nullable = false)
    val attempts: Int = 0,

    @Column(name = "FAIL_REASON", length = 4000)
    val failReason: String? = null,

    @Lob
    @Column(name = "FAIL_STACK")
    val failStack: String? = null,

    @Column(name = "IS_DLQ", nullable = false, length = 1)
    val isDlq: String = "N",

    @Lob
    @Column(name = "PAYLOAD", nullable = false, columnDefinition = "jsonb")
    val payload: String,

    @Column(name = "STAGED_AT")
    val stagedAt: LocalDateTime? = null,

    @Column(name = "LOGGED_AT")
    val loggedAt: LocalDateTime? = null,

    @Column(name = "CREATED_AT", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "UPDATED_AT", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    protected constructor() : this(
        instanceId = 0,
        taskKey = "",
        payload = "{}"
    )
}
