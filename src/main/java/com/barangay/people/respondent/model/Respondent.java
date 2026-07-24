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
@Table(name = "respondents")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Respondent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
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
}
