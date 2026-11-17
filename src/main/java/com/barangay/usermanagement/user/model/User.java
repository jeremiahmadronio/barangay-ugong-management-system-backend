package com.barangay.usermanagement.user.model;

import com.barangay.people.person.model.Person;
import com.barangay.usermanagement.department.model.Department;
import com.barangay.usermanagement.permission.model.Permission;
import com.barangay.usermanagement.role.model.Role;
import com.barangay.zshared.entity.Archivable;
import com.barangay.zshared.entity.BaseEntity;
import com.barangay.zshared.enumerated.MfaType;
import com.barangay.zshared.enumerated.UserStatus;
import com.barangay.zshared.security.EncryptedFieldConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * User entity representing system and staff accounts in the Barangay system.
 * Handles authentication credentials, role assignments, department affiliations,
 * and account lifecycle states.
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_system_email", columnList = "systemEmail"),
        @Index(name = "idx_users_username", columnList = "username"),
        @Index(name = "idx_users_person_id", columnList = "person_id"),
        @Index(name = "idx_users_role_id", columnList = "role_id"),
        @Index(name = "idx_users_status", columnList = "status"),
        @Index(name = "idx_users_lifecycle", columnList = "status, is_archived, is_deleted, is_permanent_deleted"),
        @Index(name = "idx_users_created_at", columnList = "createdAt")
})
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class User extends BaseEntity implements Archivable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "system_photo", columnDefinition = "bytea")
    private byte[] systemPhoto;

    @Column(unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(columnDefinition = "TEXT")
    private String systemEmail;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(columnDefinition = "TEXT")
    private String systemBackupEmail;

    @Column
    private Integer failedAttempts = 0;

    @Column(name = "last_failed_attempt")
    private LocalDateTime lastFailedAttempt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(columnDefinition = "TEXT")
    private String statusRemarks;

    @Column
    private Boolean isLocked = false;

    @Column
    private LocalDateTime lockUntil;

    @Column
    private LocalDateTime lastLoginAt;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "mfa_code", columnDefinition = "TEXT")
    private String mfaCode;

    @Column(name = "mfa_expiry")
    private LocalDateTime mfaExpiry;

    @Enumerated(EnumType.STRING)
    @Column(name = "mfa_type", nullable = false)
    private MfaType mfaType = MfaType.EMAIL;

    @Column(name = "is_new_account")
    private Boolean isNewAccount = true;

    @Column(name = "totp_secret", columnDefinition = "TEXT")
    @Convert(converter = EncryptedFieldConverter.class)
    private String totpSecret;

    @Column(name = "totp_enabled")
    private Boolean totpEnabled = false;

    public Boolean getTotpEnabled() {
        return Boolean.TRUE.equals(this.totpEnabled) || (this.totpSecret != null && !this.totpSecret.trim().isEmpty());
    }

    @Column(name = "is_system")
    private Boolean isSystem = false;

    @Column(name = "is_archived")
    private Boolean isArchived = false;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "archive_remarks")
    private String archiveRemarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", referencedColumnName = "id")
    private Person person;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_departments",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "dept_id")
    )
    private Set<Department> departments = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_permissions",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> customPermissions = new HashSet<>();
}
