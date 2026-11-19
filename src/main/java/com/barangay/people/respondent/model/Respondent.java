package com.barangay.people.respondent.model;

import com.barangay.people.person.model.Person;
import com.barangay.zshared.entity.BaseEntity;
import com.barangay.zshared.security.EncryptedFieldConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "respondents", indexes = {
        @Index(name = "idx_respondent_person_id", columnList = "person_id")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Respondent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "alias", columnDefinition = "TEXT")
    private String alias;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "relationship_to_complinant", columnDefinition = "TEXT")
    private String relationshipToComplainant;

    @Column(name = "is_living_with_complainant")
    private Boolean livingWithComplainant = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Person getPerson() { return person; }
    public void setPerson(Person person) { this.person = person; }

    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }

    public String getRelationshipToComplainant() { return relationshipToComplainant; }
    public void setRelationshipToComplainant(String relationshipToComplainant) { this.relationshipToComplainant = relationshipToComplainant; }

    public Boolean getLivingWithComplainant() { return livingWithComplainant; }
    public void setLivingWithComplainant(Boolean livingWithComplainant) { this.livingWithComplainant = livingWithComplainant; }
}
