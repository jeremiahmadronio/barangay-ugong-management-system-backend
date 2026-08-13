package com.barangay.ftjs.model;

import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "first_time_job_seeker_affidavit_of_loss")
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class FirstTimeJobSeekerAffidavitOfLoss extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ftjs_id")
    private FirstTimeJobSeeker ftjs;

    @Column(columnDefinition = "TEXT")
    private String reason;

    private LocalDate dateOfLoss;

    private Integer issuanceNumber;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] affidavitFiles;



    @Column(name = "or_number")
    private String orNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}
