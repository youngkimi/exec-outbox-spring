package io.majide.data.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "TB_IDEMPOTENCY",
       uniqueConstraints = @UniqueConstraint(name = "UX_IDEMPO", columnNames = {"dag_id","task_id","idempo_scope","idempo_key"}),
       indexes = {
         @Index(name = "IX_IDEMPO__DAG", columnList = "dag_id"),
         @Index(name = "IX_IDEMPO__TASK", columnList = "task_id")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Idempotency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idempo_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dag_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private DagDef dag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private TaskDef task; // NULL이면 DAG 단위 멱등

    @Column(name = "idempo_scope", length = 32, nullable = false)
    private String scope;

    @Column(name = "idempo_key", length = 200, nullable = false)
    private String key;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
