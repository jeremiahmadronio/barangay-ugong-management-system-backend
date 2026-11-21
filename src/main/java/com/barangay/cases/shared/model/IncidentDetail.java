package com.barangay.cases.shared.model;

import com.barangay.zshared.entity.BaseEntity;
import com.barangay.zshared.security.EncryptedFieldConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "incident_details", indexes = {
        @Index(name = "idx_incident_date", columnList = "incident_date")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class IncidentDetail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nature_of_complaint", columnDefinition = "TEXT")
    private String natureOfComplaint;

    @Column(name = "incident_frequency")
    private String frequency;

    @Column(name = "incident_date")
    private LocalDate dateOfIncident;

    @Column(name = "incident_time")
    private LocalTime timeOfIncident;

    @Column(length = 255, name = "incident_location")
    private String incidentLocation;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(columnDefinition = "TEXT", name = "injuries_damage_description")
    private String injuriesDamagesDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "related_barangay_service", length = 50)
    private com.barangay.zshared.enumerated.RelatedBarangayService relatedBarangayService;
}