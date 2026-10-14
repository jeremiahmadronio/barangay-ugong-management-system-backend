package com.barangay.people.resident.model;

import com.barangay.people.person.model.Person;
import com.barangay.zshared.entity.BaseEntity;
import com.barangay.zshared.enumerated.ResidentStatus;
import com.barangay.zshared.security.EncryptedFieldConverter;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "resident")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Resident extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long residentId;

    @OneToOne
    @JoinColumn(name = "person_id", nullable = false)
    @JsonBackReference
    private Person person;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "household_number", columnDefinition = "TEXT")
    private String householdNumber;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "baranggay_id_number", columnDefinition = "TEXT")
    private String barangayIdNumber;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "precinct_number", columnDefinition = "TEXT")
    private String precinctNumber;

    @Column(nullable = false, name = "is_head_of_family")
    private Boolean isHeadOfFamily = false;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "educational_attainment", columnDefinition = "TEXT")
    private String educationalAttainment;

    @Column(length = 50, name = "citizenship")
    private String citizenship = "Filipino";

    @Column(name = "residency_date")
    private LocalDate dateOfResidency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "status")
    private ResidentStatus status = ResidentStatus.ACTIVE;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "status_remarks", columnDefinition = "TEXT")
    private String statusRemarks;

    @OneToMany(mappedBy = "resident", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResidentDocument> documents = new ArrayList<>();
}
