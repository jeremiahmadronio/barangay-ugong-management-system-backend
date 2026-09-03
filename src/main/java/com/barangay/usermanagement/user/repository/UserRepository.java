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

    Optional<User> findBySystemEmail(String systemEmail);

    boolean existsBySystemEmail(String systemEmail);

    boolean existsBySystemEmailAndStatusNot(String systemEmail, com.barangay.zshared.enumerated.UserStatus status);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByPersonId(Long personId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) FROM User u WHERE u.status = :status AND (u.isArchived = false OR u.isArchived IS NULL) AND (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL)")
    long countByStatus(@org.springframework.data.repository.query.Param("status") com.barangay.zshared.enumerated.UserStatus status);

    java.util.List<User> findAllByStatus(com.barangay.zshared.enumerated.UserStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) FROM User u WHERE (u.isArchived = false OR u.isArchived IS NULL) AND (u.status IS NULL OR u.status <> com.barangay.zshared.enumerated.UserStatus.ARCHIVED) AND (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) AND u.createdAt <= :cutoff AND (u.role IS NULL OR UPPER(u.role.roleName) NOT IN ('SYSTEM_ADMIN', 'ROOTADMIN', 'ROOT_ADMIN', 'ROLE_SYSTEM_ADMIN', 'ROLE_ROOT_ADMIN'))")
    long countNonAdminUsersCreatedOnOrBefore(@org.springframework.data.repository.query.Param("cutoff") java.time.LocalDateTime cutoff);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.isDeleted = true AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) AND u.deletedAt <= :cutoff")
    java.util.List<User> findExpiredSoftDeletedUsers(@org.springframework.data.repository.query.Param("cutoff") java.time.LocalDateTime cutoff);
}
