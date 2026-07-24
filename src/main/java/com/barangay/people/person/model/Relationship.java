package com.barangay.people.person.model;

import com.barangay.zshared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "person_relationship")
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
