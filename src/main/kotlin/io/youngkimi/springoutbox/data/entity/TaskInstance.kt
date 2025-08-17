package io.youngkimi.springoutbox.data.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.OffsetDateTime

@Table(name = "TASK_INSTANCE")
@Entity
class TaskInstance(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    val id: Long? = null,

    @Column(name = "DAG_VERSION", nullable = false)
    val dagVersion: Int,

    @Column(name = "BIZ_KEY", nullable = false, length = 200)
    val bizKey: String,

    @Column(name = "STATUS", nullable = false, length = 16)
    val status: String = "RUNNING",

    @Column(name = "PRE_CNT", nullable = false)
    val preCnt: Int,

    @CreatedDate
    @Column(name = "CREATED_AT", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @LastModifiedDate
    @Column(name = "UPDATED_AT", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
) {
    // JPA 기본 생성자
    protected constructor() : this(
        id = null,
        dagVersion = 0,
        bizKey = "",
        status = "RUNNING",
        preCnt = 0
    )

    override fun toString(): String =
        "TaskInstance(id=$id, dagVersion=$dagVersion, bizKey='$bizKey', status='$status', preCnt=$preCnt)"
}
