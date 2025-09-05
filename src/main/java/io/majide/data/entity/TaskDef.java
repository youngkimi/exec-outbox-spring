package io.majide.data.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "TB_TASK_DEF",
       uniqueConstraints = @UniqueConstraint(name = "UX_TASK_DEF__DAG_TASKKEY", columnNames = {"dag_id","task_key"}),
       indexes = { @Index(name = "IX_TASK_DEF__DAG", columnList = "dag_id") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaskDef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dag_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private DagDef dag;

    @Column(name = "task_key", length = 200, nullable = false)
    private String taskKey;

    @Column(name = "task_name", length = 200, nullable = false)
    private String taskName;

    @Column(name = "handler_bean", length = 200)
    private String handlerBean;

    @Column(name = "handler_class", length = 400)
    private String handlerClass;

    @Column(name = "timeout_sec", nullable = false)
    private Integer timeoutSec;

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts;

    @Column(name = "retry_backoff_ms", nullable = false)
    private Integer retryBackoffMs;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
