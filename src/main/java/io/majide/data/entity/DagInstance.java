package io.youngkimi.springoutbox.data.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "TB_DAG_INSTANCE",
       indexes = {
         @Index(name = "UX_INSTANCE__DAG_PERIOD", columnList = "dag_id, period_key", unique = true),
         @Index(name = "IX_INSTANCE__DAG", columnList = "dag_id")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DagInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "instance_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dag_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private DagDef dag;

    @Column(name = "dag_name_snapshot", length = 200, nullable = false)
    private String dagNameSnapshot;

    @Column(name = "dag_version", nullable = false)
    private Integer dagVersion;

    @Column(name = "period_key", length = 32, nullable = false)
    private String periodKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 24, nullable = false)
    private DagInstanceStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Lob
    @Column(name = "input_params")
    private String inputParamsJson; // JSON/CLOB/JSONB 호환
}
