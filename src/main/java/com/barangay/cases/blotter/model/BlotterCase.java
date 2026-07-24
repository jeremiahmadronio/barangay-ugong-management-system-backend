package com.barangay.cases.blotter.model;

import com.barangay.zshared.entity.BaseEntity;
import com.barangay.zshared.security.EncryptedFieldConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "blotter_cases")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BlotterCase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_number", unique = true, length = 50)
    private String caseNumber;

    @Column(name = "incident_type", length = 100)
    private String incidentType;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "narrative", columnDefinition = "TEXT")
    private String narrative;

    @Column(length = 50)
    private String status;
}
