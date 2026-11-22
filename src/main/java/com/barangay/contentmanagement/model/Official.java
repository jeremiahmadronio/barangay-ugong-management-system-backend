package com.barangay.contentmanagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing barangay and SK officials published in the leadership directory.
 */
@Entity
@Table(
    name = "officials",
    indexes = {
        @Index(name = "idx_officials_created_at", columnList = "createdAt"),
        @Index(name = "idx_officials_is_archived", columnList = "is_archived"),
        @Index(name = "idx_officials_type", columnList = "type")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Official {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String position;

    @Column(nullable = false)
    private String type; // e.g. CAPTAIN, KAGAWAD, SK, SECRETARY, TREASURER, STAFF

    @Column
    private String imageUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "is_archived")
    private Boolean isArchived = false;
}
