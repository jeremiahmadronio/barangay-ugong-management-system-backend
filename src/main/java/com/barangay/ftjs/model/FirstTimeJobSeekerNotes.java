package com.barangay.ftjs.model;

import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "first_time_job_seeker_notes")
@Data
public class FirstTimeJobSeekerNotes extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ftjs_id")
    private FirstTimeJobSeeker ftjs;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;


}
