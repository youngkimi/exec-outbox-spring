package io.youngkimi.springoutbox.data.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TB_TASK_DEP",
       indexes = { @Index(name = "IX_TASK_DEP__TO", columnList = "dag_id, to_task_id") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaskDep {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dag_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private DagDef dag;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_task_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private TaskDef fromTask;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_task_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private TaskDef toTask;

    @Enumerated(EnumType.STRING)
    @Column(name = "is_optional", length = 1, nullable = false)
    private Yn optional; // Y/N

    // PK: (dag_id, from_task_id, to_task_id)
    @Id
    @Column(name = "pk_dummy")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pkDummy; // 기술적 PK (복합키 대신 단일 PK 선택, 운영 편의)
}
