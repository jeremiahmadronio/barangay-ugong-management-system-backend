package com.barangay.usermanagement.user.repository;

import com.barangay.usermanagement.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * The main access point for all User data.
 * It combines standard Spring Boot tools with our Manual/Complex
 * queries from "UserRepositoryCustom".
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE LOWER(TRIM(u.systemEmail)) = LOWER(TRIM(:systemEmail)) AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) " +
            "ORDER BY CASE " +
            "  WHEN (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isArchived = false OR u.isArchived IS NULL) AND u.status = com.barangay.zshared.enumerated.UserStatus.ACTIVE THEN 0 " +
            "  WHEN (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isArchived = false OR u.isArchived IS NULL) THEN 1 " +
            "  WHEN (u.isDeleted = false OR u.isDeleted IS NULL) THEN 2 " +
            "  ELSE 3 " +
            "END ASC, u.id DESC")
    java.util.List<User> findAllActiveOrRecentBySystemEmail(@org.springframework.data.repository.query.Param("systemEmail") String systemEmail);

    default Optional<User> findBySystemEmail(String systemEmail) {
        if (systemEmail == null || systemEmail.isBlank()) return Optional.empty();
        return findAllActiveOrRecentBySystemEmail(systemEmail.trim()).stream().findFirst();
    }

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) > 0 FROM User u WHERE LOWER(TRIM(u.systemEmail)) = LOWER(TRIM(:systemEmail)) AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL)")
    boolean existsActiveOrRecentDeletedBySystemEmail(@org.springframework.data.repository.query.Param("systemEmail") String systemEmail);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) > 0 FROM User u WHERE LOWER(TRIM(u.systemEmail)) = LOWER(TRIM(:systemEmail)) AND u.id <> :excludeUserId AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL)")
    boolean existsActiveOrRecentDeletedBySystemEmailExcludingUser(@org.springframework.data.repository.query.Param("systemEmail") String systemEmail, @org.springframework.data.repository.query.Param("excludeUserId") Long excludeUserId);


    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) > 0 FROM User u WHERE LOWER(TRIM(u.username)) = LOWER(TRIM(:username)) AND u.id <> :excludeUserId AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL)")
    boolean existsByUsernameExcludingUser(@org.springframework.data.repository.query.Param("username") String username, @org.springframework.data.repository.query.Param("excludeUserId") Long excludeUserId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) > 0 FROM User u WHERE u.person.id = :personId AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL)")
    boolean existsActiveOrRecentDeletedByPersonId(@org.springframework.data.repository.query.Param("personId") Long personId);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.person.id = :personId AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) ORDER BY u.id DESC")
    java.util.List<User> findAllByPersonId(@org.springframework.data.repository.query.Param("personId") Long personId);

    default Optional<User> findFirstByPersonId(Long personId) {
        if (personId == null) return Optional.empty();
        return findAllByPersonId(personId).stream().findFirst();
    }


    @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) FROM User u LEFT JOIN u.role r WHERE (u.isArchived = false OR u.isArchived IS NULL) AND (u.status IS NULL OR u.status <> com.barangay.zshared.enumerated.UserStatus.ARCHIVED) AND (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) AND (u.isSystem = false OR u.isSystem IS NULL) AND (r.roleName IS NULL OR UPPER(r.roleName) NOT IN ('SYSTEM_ADMIN', 'ROOTADMIN', 'ROOT_ADMIN', 'ROLE_SYSTEM_ADMIN', 'ROLE_ROOT_ADMIN')) AND (u.username IS NULL OR LOWER(u.username) <> 'superadmin')")
    long countTotalUsers();

    java.util.List<User> findAllByStatus(com.barangay.zshared.enumerated.UserStatus status);


    @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) FROM User u WHERE (u.isArchived = false OR u.isArchived IS NULL) AND (u.status IS NULL OR u.status <> com.barangay.zshared.enumerated.UserStatus.ARCHIVED) AND (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) AND u.createdAt BETWEEN :start AND :end AND (u.role IS NULL OR UPPER(u.role.roleName) NOT IN ('SYSTEM_ADMIN', 'ROOTADMIN', 'ROOT_ADMIN', 'ROLE_SYSTEM_ADMIN', 'ROLE_ROOT_ADMIN'))")
    long countNonAdminUsersCreatedBetween(@org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start, @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.isDeleted = true AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) AND u.deletedAt <= :cutoff")
    java.util.List<User> findExpiredSoftDeletedUsers(@org.springframework.data.repository.query.Param("cutoff") java.time.LocalDateTime cutoff);
}
