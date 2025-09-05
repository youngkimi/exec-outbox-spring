package io.majide.data.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "TB_SCHEDULE_DEF",
       indexes = { @Index(name = "IX_SCHEDULE_DEF__ACTIVE_NEXT", columnList = "is_active, next_fire_at") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScheduleDef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dag_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private DagDef dag;

    @Column(name = "cron_expr", length = 120, nullable = false)
    private String cronExpr;

    @Column(name = "tzid", length = 64, nullable = false)
    private String tzid;

    @Enumerated(EnumType.STRING)
    @Column(name = "is_active", length = 1, nullable = false)
    private Yn active; // Y/N

    @Column(name = "next_fire_at")
    private Instant nextFireAt;

    @Column(name = "last_fire_at")
    private Instant lastFireAt;

    @Column(name = "dedupe_window_s", nullable = false)
    private Integer dedupeWindowSeconds;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
