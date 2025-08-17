package io.youngkimi.springoutbox.data.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "TB_BAT_OUTBOX_EVENT")
public class OutboxEvent {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name="RUN_ID", nullable=false) private Long runId;
	@Column(name="INSTANCE_ID", nullable=false) private Long instanceId;
	@Column(name="TASK_KEY", nullable=false) private String taskKey;
	@Column(name="STATUS", nullable=false) private String status;
	@Column(name="WORKER_ID") private String workerId;
	@Column(name="VISIBLE_AT", nullable=false) private OffsetDateTime visibleAt;
	@Column(name="ATTEMPTS", nullable=false) private Integer attempts;
	@Column(name="IS_DLQ", nullable=false) private boolean dlq;

	public Long getId() { return id; }
	public Long getInstanceId() { return instanceId; }
	public String getTaskKey() { return taskKey; }
	public String getStatus() { return status; }
}

