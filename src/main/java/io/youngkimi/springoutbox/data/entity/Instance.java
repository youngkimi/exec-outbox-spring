package io.youngkimi.springoutbox.data.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "TB_BAT_INSTANCE")
public class Instance {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name="RUN_ID", nullable=false) private Long runId;
	@Column(name="DAG_VERSION", nullable=false) private Long dagVersion;
	@Column(name="BIZ_KEY", nullable=false) private String bizKey;
	@Column(name="TASK_KEY", nullable=false) private String taskKey;
	@Column(name="STATUS", nullable=false) private String status;
	@Column(name="PRE_CNT", nullable=false) private Integer preCnt;
	@Column(name="ENQUEUED_AT") private OffsetDateTime enqueuedAt;

	public Long getId() { return id; }
	public Long getRunId() { return runId; }
	public String getTaskKey() { return taskKey; }
	public Integer getPreCnt() { return preCnt; }
	public String getStatus() { return status; }
}
