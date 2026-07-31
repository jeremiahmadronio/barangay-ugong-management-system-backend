package com.barangay.people.complainant.model;

import com.barangay.people.person.model.Person;
import com.barangay.zshared.entity.BaseEntity;
import com.barangay.zshared.security.EncryptedFieldConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "complainants")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Complainant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "relationship_to_child", columnDefinition = "TEXT")
    private String relationshipToChild;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Person getPerson() { return person; }
    public void setPerson(Person person) { this.person = person; }

    public String getRelationshipToChild() { return relationshipToChild; }
    public void setRelationshipToChild(String relationshipToChild) { this.relationshipToChild = relationshipToChild; }
}
