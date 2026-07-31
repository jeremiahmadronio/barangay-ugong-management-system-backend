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

@Entity
@Table(name = "users")
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

    @Column(unique = true, columnDefinition = "TEXT")
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

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "person_id", referencedColumnName = "id", unique = true)
    private Person person;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_departments",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "dept_id")
    )
    private Set<Department> departments = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_permissions",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> customPermissions = new HashSet<>();

    // Explicit Getters and Setters to guarantee compilation across all environments
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public byte[] getSystemPhoto() { return systemPhoto; }
    public void setSystemPhoto(byte[] systemPhoto) { this.systemPhoto = systemPhoto; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getSystemEmail() { return systemEmail; }
    public void setSystemEmail(String systemEmail) { this.systemEmail = systemEmail; }

    public String getSystemBackupEmail() { return systemBackupEmail; }
    public void setSystemBackupEmail(String systemBackupEmail) { this.systemBackupEmail = systemBackupEmail; }

    public Integer getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(Integer failedAttempts) { this.failedAttempts = failedAttempts; }

    public LocalDateTime getLastFailedAttempt() { return lastFailedAttempt; }
    public void setLastFailedAttempt(LocalDateTime lastFailedAttempt) { this.lastFailedAttempt = lastFailedAttempt; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public String getStatusRemarks() { return statusRemarks; }
    public void setStatusRemarks(String statusRemarks) { this.statusRemarks = statusRemarks; }

    public Boolean getIsLocked() { return isLocked; }
    public void setIsLocked(Boolean isLocked) { this.isLocked = isLocked; }

    public LocalDateTime getLockUntil() { return lockUntil; }
    public void setLockUntil(LocalDateTime lockUntil) { this.lockUntil = lockUntil; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public String getMfaCode() { return mfaCode; }
    public void setMfaCode(String mfaCode) { this.mfaCode = mfaCode; }

    public LocalDateTime getMfaExpiry() { return mfaExpiry; }
    public void setMfaExpiry(LocalDateTime mfaExpiry) { this.mfaExpiry = mfaExpiry; }

    public MfaType getMfaType() { return mfaType; }
    public void setMfaType(MfaType mfaType) { this.mfaType = mfaType; }

    public Boolean getIsNewAccount() { return isNewAccount; }
    public void setIsNewAccount(Boolean isNewAccount) { this.isNewAccount = isNewAccount; }

    public String getTotpSecret() { return totpSecret; }
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }

    public Boolean getTotpEnabled() { return totpEnabled; }
    public void setTotpEnabled(Boolean totpEnabled) { this.totpEnabled = totpEnabled; }

    public Boolean getIsSystem() { return isSystem; }
    public void setIsSystem(Boolean isSystem) { this.isSystem = isSystem; }

    public Boolean getIsArchived() { return isArchived; }
    public void setIsArchived(Boolean isArchived) { this.isArchived = isArchived; }

    public LocalDateTime getArchivedAt() { return archivedAt; }
    public void setArchivedAt(LocalDateTime archivedAt) { this.archivedAt = archivedAt; }

    public String getArchiveRemarks() { return archiveRemarks; }
    public void setArchiveRemarks(String archiveRemarks) { this.archiveRemarks = archiveRemarks; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Person getPerson() { return person; }
    public void setPerson(Person person) { this.person = person; }

    public Set<Department> getDepartments() { return departments; }
    public void setDepartments(Set<Department> departments) { this.departments = departments; }

    public Set<Permission> getCustomPermissions() { return customPermissions; }
    public void setCustomPermissions(Set<Permission> customPermissions) { this.customPermissions = customPermissions; }
}
