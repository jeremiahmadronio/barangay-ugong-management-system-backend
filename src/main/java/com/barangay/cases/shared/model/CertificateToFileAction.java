package com.barangay.cases.shared.model;

import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "certificates_to_file_action")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CertificateToFileAction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false, unique = true)
    private Case cases;

    @Column(columnDefinition = "TEXT", nullable = false, name = "grounds")
    private String grounds;

    @Column(name = "subject_of_litigation", nullable = false)
    private String subjectOfLitigation;

    @Column(name = "control_number", length = 50, unique = true)
    private String controlNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by")
    private User issuedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

}