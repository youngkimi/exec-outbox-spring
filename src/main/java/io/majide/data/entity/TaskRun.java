package io.youngkimi.springoutbox.data.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "TB_TASK_RUN",
       uniqueConstraints = @UniqueConstraint(name = "UX_TASK_RUN__UNIQ", columnNames = {"instance_id","task_id"}),
       indexes = {
         @Index(name = "IX_TASK_RUN__READY", columnList = "status, scheduled_at, lease_expire_at"),
         @Index(name = "IX_TASK_RUN__INSTANCE", columnList = "instance_id, status")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaskRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "run_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instance_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private DagInstance instance;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private TaskDef task;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 24, nullable = false)
    private TaskRunStatus status;

    @Column(name = "attempt", nullable = false)
    private Integer attempt;

    @Column(name = "lease_owner", length = 128)
    private String leaseOwner;

    @Column(name = "lease_expire_at")
    private OffsetDateTime leaseExpireAt;

    @Column(name = "heartbeat_at")
    private OffsetDateTime heartbeatAt;

    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Column(name = "error_code", length = 120)
    private String errorCode;

    @Lob
    @Column(name = "error_msg")
    private String errorMsg;

    @Lob
    @Column(name = "payload_in")
    private String payloadInJson;

    @Lob
    @Column(name = "payload_out")
    private String payloadOutJson;
}
