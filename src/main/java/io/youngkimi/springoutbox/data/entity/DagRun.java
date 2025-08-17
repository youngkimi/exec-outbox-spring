package io.youngkimi.springoutbox.data.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "TB_BAT_DAG_RUN")
public class DagRun {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "RUN_ID")
	private Long runId;

	@Column(name = "DAG_VERSION", nullable = false)
	private Long dagVersion;

	@Column(name = "BIZ_KEY", nullable = false)
	private String bizKey;

	@Column(name = "STATUS", nullable = false)
	private String status;

	@Column(name = "CREATED_AT", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "UPDATED_AT", nullable = false)
	private OffsetDateTime updatedAt;

	// getters/setters
	public Long getRunId() { return runId; }
	public Long getDagVersion() { return dagVersion; }
	public void setDagVersion(Long dagVersion) { this.dagVersion = dagVersion; }
	public String getBizKey() { return bizKey; }
	public void setBizKey(String bizKey) { this.bizKey = bizKey; }
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
}

