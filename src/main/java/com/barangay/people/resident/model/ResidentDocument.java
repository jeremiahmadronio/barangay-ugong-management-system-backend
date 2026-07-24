package com.barangay.people.resident.model;

import com.barangay.zshared.entity.BaseEntity;
import com.barangay.zshared.enumerated.ResidentDocumentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resident_documents")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResidentDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id", nullable = false)
    private Resident resident;

    @Column(nullable = false, name = "document_name")
    private String documentName;

    @Column(nullable = false, name = "document_type")
    private String documentType;

    @Lob
    @Column(name = "file_data", columnDefinition = "bytea")
    private byte[] fileData;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ResidentDocumentStatus status;
}
