package com.barangay.people.witness.model;

import com.barangay.cases.blotter.model.BlotterCase;
import com.barangay.people.person.model.Person;
import com.barangay.zshared.entity.BaseEntity;
import com.barangay.zshared.security.EncryptedFieldConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "witnesses")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Witness extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private BlotterCase blotterCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(columnDefinition = "TEXT")
    private String testimony;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "testimony_file", columnDefinition = "bytea")
    private byte[] testimonyFile;
}
