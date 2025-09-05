package io.majide.data.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.Instant;

@Entity
@Table(name = "TB_DAG_DEF",
       indexes = {
         @Index(name = "UX_DAG_DEF__NAME_VER", columnList = "dag_name, version", unique = true)
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DagDef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dag_id")
    private Long id;

    @Column(name = "dag_name", length = 200, nullable = false)
    private String name;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "is_active", length = 1, nullable = false)
    private Yn active; // Y/N

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
