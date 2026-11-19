package com.barangay.people.person.model;

import com.barangay.zshared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "person_relationship", indexes = {
        @Index(name = "idx_rel_person_id", columnList = "person_id"),
        @Index(name = "idx_rel_relative_id", columnList = "relative_id"),
        @Index(name = "idx_rel_pair", columnList = "person_id, relative_id")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Relationship extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "relative_id", nullable = false)
    private Person relative;

    @Column(name = "relationship_type", length = 50, nullable = false)
    private String relationshipType;
}
