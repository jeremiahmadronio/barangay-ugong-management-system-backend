package com.barangay.zshared.audit.model;

import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.entity.BaseEntity;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import com.barangay.zshared.security.EncryptedFieldConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


/**
 * Stores a record of every significant action done inside the system.
 * Tracks who did it, what they did, when, and what data changed.
 */
@Table(name = "audit_logs")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class AuditLogs extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditModule module;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(columnDefinition = "TEXT", nullable = false, name = "action_taken")
    private String actionTaken;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(columnDefinition = "TEXT")
    private String reason;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(columnDefinition = "TEXT")
    private String ipAddress;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String oldValue;


    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String newValue;

}
