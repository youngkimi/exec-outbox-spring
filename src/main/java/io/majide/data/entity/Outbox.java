package io.youngkimi.springoutbox.data.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "TB_OUTBOX",
       indexes = { @Index(name = "UX_OUTBOX__EVENT_KEY", columnList = "event_key", unique = true) })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Outbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long id;

    @Column(name = "aggregate_type", length = 120, nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", length = 120, nullable = false)
    private String aggregateId;

    @Column(name = "event_type", length = 120, nullable = false)
    private String eventType;

    @Column(name = "event_key", length = 200)
    private String eventKey;

    @Lob
    @Column(name = "payload")
    private String payloadJson;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private OutboxStatus status;

    @Column(name = "attempt", nullable = false)
    private Integer attempt;
}
