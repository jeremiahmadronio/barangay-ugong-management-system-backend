package com.barangay.usermanagement.user.repository;

import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.enumerated.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for managing User entity database queries,
 * lookup operations, lifecycle filtering, and statistical counts.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findAllByStatus(UserStatus status);

    @Query("SELECT COUNT(u) FROM User u WHERE (u.status = com.barangay.zshared.enumerated.UserStatus.ARCHIVED OR u.isArchived = true) AND (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL)")
    long countArchivedUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.isDeleted = true AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL)")
    long countTrashUsers();

    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.person " +
            "LEFT JOIN FETCH u.role " +
            "LEFT JOIN FETCH u.departments " +
            "WHERE u.status = :status AND (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL)")
    List<User> findArchivedUsersWithDetails(@Param("status") UserStatus status);

    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.person " +
            "LEFT JOIN FETCH u.role " +
            "LEFT JOIN FETCH u.departments " +
            "WHERE u.isDeleted = true AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL)")
    List<User> findTrashUsersWithDetails();

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    default List<User> findAllActiveOrRecentBySystemEmail(String systemEmail) {
        return findAllActiveOrRecentBySystemEmail(systemEmail, UserStatus.ACTIVE);
    }

    default Optional<User> findBySystemEmail(String systemEmail) {
        if (systemEmail == null || systemEmail.isBlank()) return Optional.empty();
        return findAllActiveOrRecentBySystemEmail(systemEmail.trim()).stream().findFirst();
    }

    default Optional<User> findFirstByPersonId(Long personId) {
        if (personId == null) return Optional.empty();
        return findAllByPersonId(personId).stream().findFirst();
    }

    default Optional<User> findByEmailWithRoleAndDepartments(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        return findByEmailWithRoleAndDepartmentsInternal(email.trim(), UserStatus.ARCHIVED, UserStatus.ACTIVE)
                .stream().findFirst();
    }

    default long countTotalUsers() {
        return countTotalUsersInternal(UserStatus.ARCHIVED);
    }

    default long countNonAdminUsersCreatedBetween(LocalDateTime start, LocalDateTime end) {
        return countNonAdminUsersCreatedBetweenInternal(start, end, UserStatus.ARCHIVED);
    }

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE LOWER(TRIM(u.systemEmail)) = LOWER(TRIM(:systemEmail)) AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL)")
    boolean existsActiveOrRecentDeletedBySystemEmail(@Param("systemEmail") String systemEmail);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE LOWER(TRIM(u.systemEmail)) = LOWER(TRIM(:systemEmail)) AND u.id <> :excludeUserId AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL)")
    boolean existsActiveOrRecentDeletedBySystemEmailExcludingUser(@Param("systemEmail") String systemEmail, @Param("excludeUserId") Long excludeUserId);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE LOWER(TRIM(u.username)) = LOWER(TRIM(:username)) AND u.id <> :excludeUserId AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL)")
    boolean existsByUsernameExcludingUser(@Param("username") String username, @Param("excludeUserId") Long excludeUserId);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.person.id = :personId AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL)")
    boolean existsActiveOrRecentDeletedByPersonId(@Param("personId") Long personId);

    @Query("SELECT u FROM User u WHERE u.person.id = :personId AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) ORDER BY u.id DESC")
    List<User> findAllByPersonId(@Param("personId") Long personId);

    @Query("SELECT u FROM User u WHERE u.isDeleted = true AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) AND u.deletedAt <= :cutoff")
    List<User> findExpiredSoftDeletedUsers(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.role " +
            "LEFT JOIN FETCH u.departments " +
            "WHERE LOWER(TRIM(u.systemEmail)) = LOWER(TRIM(:email)) " +
            "AND (u.isArchived = false OR u.isArchived IS NULL) " +
            "AND (u.status IS NULL OR u.status <> :archivedStatus) " +
            "AND (u.isDeleted = false OR u.isDeleted IS NULL) " +
            "AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) " +
            "ORDER BY CASE WHEN u.status = :activeStatus THEN 0 ELSE 1 END ASC, u.id DESC")
    List<User> findByEmailWithRoleAndDepartmentsInternal(
            @Param("email") String email,
            @Param("archivedStatus") UserStatus archivedStatus,
            @Param("activeStatus") UserStatus activeStatus
    );

    @Query("SELECT u FROM User u WHERE LOWER(TRIM(u.systemEmail)) = LOWER(TRIM(:systemEmail)) AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) " +
            "ORDER BY CASE " +
            "  WHEN (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isArchived = false OR u.isArchived IS NULL) AND u.status = :activeStatus THEN 0 " +
            "  WHEN (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isArchived = false OR u.isArchived IS NULL) THEN 1 " +
            "  WHEN (u.isDeleted = false OR u.isDeleted IS NULL) THEN 2 " +
            "  ELSE 3 " +
            "END ASC, u.id DESC")
    List<User> findAllActiveOrRecentBySystemEmail(@Param("systemEmail") String systemEmail, @Param("activeStatus") UserStatus activeStatus);

    @Query("SELECT COUNT(u) FROM User u LEFT JOIN u.role r WHERE (u.isArchived = false OR u.isArchived IS NULL) AND (u.status IS NULL OR u.status <> :archivedStatus) AND (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) AND (u.isSystem = false OR u.isSystem IS NULL) AND (r.roleName IS NULL OR UPPER(r.roleName) NOT IN ('SYSTEM_ADMIN', 'ROOTADMIN', 'ROOT_ADMIN', 'ROLE_SYSTEM_ADMIN', 'ROLE_ROOT_ADMIN')) AND (u.username IS NULL OR LOWER(u.username) <> 'superadmin')")
    long countTotalUsersInternal(@Param("archivedStatus") UserStatus archivedStatus);

    @Query("SELECT COUNT(u) FROM User u LEFT JOIN u.role r WHERE (u.isArchived = false OR u.isArchived IS NULL) AND (u.status IS NULL OR u.status <> :archivedStatus) AND (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) AND (u.isSystem = false OR u.isSystem IS NULL) AND (r.roleName IS NULL OR UPPER(r.roleName) NOT IN ('SYSTEM_ADMIN', 'ROOTADMIN', 'ROOT_ADMIN', 'ROLE_SYSTEM_ADMIN', 'ROLE_ROOT_ADMIN')) AND (u.username IS NULL OR LOWER(u.username) <> 'superadmin') AND u.createdAt >= :start AND u.createdAt <= :end")
    long countTotalUsersBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("archivedStatus") UserStatus archivedStatus);

    @Query("SELECT COUNT(u) FROM User u LEFT JOIN u.role r WHERE (u.isArchived = false OR u.isArchived IS NULL) AND (u.status IS NULL OR u.status <> :archivedStatus) AND (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) AND (u.isSystem = false OR u.isSystem IS NULL) AND (r.roleName IS NULL OR UPPER(r.roleName) NOT IN ('SYSTEM_ADMIN', 'ROOTADMIN', 'ROOT_ADMIN', 'ROLE_SYSTEM_ADMIN', 'ROLE_ROOT_ADMIN')) AND (u.username IS NULL OR LOWER(u.username) <> 'superadmin') AND u.status = :activeStatus")
    long countActiveUsersInternal(@Param("archivedStatus") UserStatus archivedStatus, @Param("activeStatus") UserStatus activeStatus);

    @Query("SELECT COUNT(u) FROM User u LEFT JOIN u.role r WHERE (u.isArchived = false OR u.isArchived IS NULL) AND (u.status IS NULL OR u.status <> :archivedStatus) AND (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) AND (u.isSystem = false OR u.isSystem IS NULL) AND (r.roleName IS NULL OR UPPER(r.roleName) NOT IN ('SYSTEM_ADMIN', 'ROOTADMIN', 'ROOT_ADMIN', 'ROLE_SYSTEM_ADMIN', 'ROLE_ROOT_ADMIN')) AND (u.username IS NULL OR LOWER(u.username) <> 'superadmin') AND u.status = :activeStatus AND u.createdAt >= :start AND u.createdAt <= :end")
    long countActiveUsersBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("archivedStatus") UserStatus archivedStatus, @Param("activeStatus") UserStatus activeStatus);

    @Query("SELECT COUNT(u) FROM User u LEFT JOIN u.role r WHERE (u.isArchived = false OR u.isArchived IS NULL) AND (u.status IS NULL OR u.status <> :archivedStatus) AND (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) AND (u.isSystem = false OR u.isSystem IS NULL) AND (r.roleName IS NULL OR UPPER(r.roleName) NOT IN ('SYSTEM_ADMIN', 'ROOTADMIN', 'ROOT_ADMIN', 'ROLE_SYSTEM_ADMIN', 'ROLE_ROOT_ADMIN')) AND (u.username IS NULL OR LOWER(u.username) <> 'superadmin') AND u.isLocked = true")
    long countLockedUsersInternal(@Param("archivedStatus") UserStatus archivedStatus);

    @Query("SELECT COUNT(u) FROM User u LEFT JOIN u.role r WHERE (u.isArchived = false OR u.isArchived IS NULL) AND (u.status IS NULL OR u.status <> :archivedStatus) AND (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) AND (u.isSystem = false OR u.isSystem IS NULL) AND (r.roleName IS NULL OR UPPER(r.roleName) NOT IN ('SYSTEM_ADMIN', 'ROOTADMIN', 'ROOT_ADMIN', 'ROLE_SYSTEM_ADMIN', 'ROLE_ROOT_ADMIN')) AND (u.username IS NULL OR LOWER(u.username) <> 'superadmin') AND u.isLocked = true AND u.createdAt >= :start AND u.createdAt <= :end")
    long countLockedUsersBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("archivedStatus") UserStatus archivedStatus);

    @Query("SELECT COUNT(u) FROM User u LEFT JOIN u.role r WHERE (u.isArchived = false OR u.isArchived IS NULL) AND (u.status IS NULL OR u.status <> :archivedStatus) AND (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) AND (u.isSystem = false OR u.isSystem IS NULL) AND (r.roleName IS NULL OR UPPER(r.roleName) NOT IN ('SYSTEM_ADMIN', 'ROOTADMIN', 'ROOT_ADMIN', 'ROLE_SYSTEM_ADMIN', 'ROLE_ROOT_ADMIN')) AND (u.username IS NULL OR LOWER(u.username) <> 'superadmin') AND u.status = :inactiveStatus")
    long countInactiveUsersInternal(@Param("archivedStatus") UserStatus archivedStatus, @Param("inactiveStatus") UserStatus inactiveStatus);

    @Query("SELECT COUNT(u) FROM User u LEFT JOIN u.role r WHERE (u.isArchived = false OR u.isArchived IS NULL) AND (u.status IS NULL OR u.status <> :archivedStatus) AND (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) AND (u.isSystem = false OR u.isSystem IS NULL) AND (r.roleName IS NULL OR UPPER(r.roleName) NOT IN ('SYSTEM_ADMIN', 'ROOTADMIN', 'ROOT_ADMIN', 'ROLE_SYSTEM_ADMIN', 'ROLE_ROOT_ADMIN')) AND (u.username IS NULL OR LOWER(u.username) <> 'superadmin') AND u.status = :inactiveStatus AND u.createdAt >= :start AND u.createdAt <= :end")
    long countInactiveUsersBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("archivedStatus") UserStatus archivedStatus, @Param("inactiveStatus") UserStatus inactiveStatus);

    @Query("SELECT COUNT(u) FROM User u WHERE (u.isArchived = false OR u.isArchived IS NULL) AND (u.status IS NULL OR u.status <> :archivedStatus) AND (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) AND u.createdAt BETWEEN :start AND :end AND (u.role IS NULL OR UPPER(u.role.roleName) NOT IN ('SYSTEM_ADMIN', 'ROOTADMIN', 'ROOT_ADMIN', 'ROLE_SYSTEM_ADMIN', 'ROLE_ROOT_ADMIN'))")
    long countNonAdminUsersCreatedBetweenInternal(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("archivedStatus") UserStatus archivedStatus);

    @Query("SELECT COUNT(u) FROM User u LEFT JOIN u.role r WHERE (u.isArchived = false OR u.isArchived IS NULL) AND (u.status IS NULL OR u.status <> :archivedStatus) AND (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) AND (u.isSystem = false OR u.isSystem IS NULL) AND (u.username IS NULL OR LOWER(u.username) <> 'superadmin') AND (r.roleName IS NOT NULL AND UPPER(r.roleName) NOT LIKE '%ADMIN%')")
    long countTotalSystemUsers(@Param("archivedStatus") UserStatus archivedStatus);

    @Query("SELECT COUNT(u) FROM User u LEFT JOIN u.role r WHERE (u.isArchived = false OR u.isArchived IS NULL) AND (u.status IS NULL OR u.status <> :archivedStatus) AND (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) AND (u.isSystem = false OR u.isSystem IS NULL) AND (u.username IS NULL OR LOWER(u.username) <> 'superadmin') AND (r.roleName IS NOT NULL AND UPPER(r.roleName) NOT LIKE '%ADMIN%') AND u.createdAt >= :start AND u.createdAt <= :end")
    long countTotalSystemUsersBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("archivedStatus") UserStatus archivedStatus);

    @Query("SELECT COUNT(u) FROM User u LEFT JOIN u.role r WHERE (u.isArchived = false OR u.isArchived IS NULL) AND (u.status IS NULL OR u.status <> :archivedStatus) AND (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) AND (u.isSystem = false OR u.isSystem IS NULL) AND (u.username IS NULL OR LOWER(u.username) <> 'superadmin') AND (r.roleName IS NOT NULL AND UPPER(r.roleName) NOT LIKE '%ADMIN%') AND u.status = :activeStatus AND (u.isLocked = false OR u.isLocked IS NULL)")
    long countActiveSystemUsers(@Param("archivedStatus") UserStatus archivedStatus, @Param("activeStatus") UserStatus activeStatus);

    @Query("SELECT COUNT(u) FROM User u LEFT JOIN u.role r WHERE (u.isArchived = false OR u.isArchived IS NULL) AND (u.status IS NULL OR u.status <> :archivedStatus) AND (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) AND (u.isSystem = false OR u.isSystem IS NULL) AND (u.username IS NULL OR LOWER(u.username) <> 'superadmin') AND (r.roleName IS NOT NULL AND UPPER(r.roleName) NOT LIKE '%ADMIN%') AND u.status = :activeStatus AND (u.isLocked = false OR u.isLocked IS NULL) AND u.createdAt >= :start AND u.createdAt <= :end")
    long countActiveSystemUsersBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("archivedStatus") UserStatus archivedStatus, @Param("activeStatus") UserStatus activeStatus);

    @Query("SELECT COUNT(u) FROM User u LEFT JOIN u.role r WHERE (u.isArchived = false OR u.isArchived IS NULL) AND (u.status IS NULL OR u.status <> :archivedStatus) AND (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) AND (u.isSystem = false OR u.isSystem IS NULL) AND (u.username IS NULL OR LOWER(u.username) <> 'superadmin') AND (r.roleName IS NOT NULL AND UPPER(r.roleName) NOT LIKE '%ADMIN%') AND (u.isLocked = true OR u.status = :lockedStatus)")
    long countLockedSystemUsers(@Param("archivedStatus") UserStatus archivedStatus, @Param("lockedStatus") UserStatus lockedStatus);

    @Query("SELECT COUNT(u) FROM User u LEFT JOIN u.role r WHERE (u.isArchived = false OR u.isArchived IS NULL) AND (u.status IS NULL OR u.status <> :archivedStatus) AND (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) AND (u.isSystem = false OR u.isSystem IS NULL) AND (u.username IS NULL OR LOWER(u.username) <> 'superadmin') AND (r.roleName IS NOT NULL AND UPPER(r.roleName) NOT LIKE '%ADMIN%') AND (u.isLocked = true OR u.status = :lockedStatus) AND u.createdAt >= :start AND u.createdAt <= :end")
    long countLockedSystemUsersBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("archivedStatus") UserStatus archivedStatus, @Param("lockedStatus") UserStatus lockedStatus);

    @Query("SELECT COUNT(u) FROM User u LEFT JOIN u.role r WHERE (u.isArchived = false OR u.isArchived IS NULL) AND (u.status IS NULL OR u.status <> :archivedStatus) AND (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) AND (u.isSystem = false OR u.isSystem IS NULL) AND (u.username IS NULL OR LOWER(u.username) <> 'superadmin') AND (r.roleName IS NOT NULL AND UPPER(r.roleName) NOT LIKE '%ADMIN%') AND u.status = :inactiveStatus")
    long countInactiveSystemUsers(@Param("archivedStatus") UserStatus archivedStatus, @Param("inactiveStatus") UserStatus inactiveStatus);

    @Query("SELECT COUNT(u) FROM User u LEFT JOIN u.role r WHERE (u.isArchived = false OR u.isArchived IS NULL) AND (u.status IS NULL OR u.status <> :archivedStatus) AND (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) AND (u.isSystem = false OR u.isSystem IS NULL) AND (u.username IS NULL OR LOWER(u.username) <> 'superadmin') AND (r.roleName IS NOT NULL AND UPPER(r.roleName) NOT LIKE '%ADMIN%') AND u.status = :inactiveStatus AND u.createdAt >= :start AND u.createdAt <= :end")
    long countInactiveSystemUsersBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("archivedStatus") UserStatus archivedStatus, @Param("inactiveStatus") UserStatus inactiveStatus);

    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.person p " +
            "LEFT JOIN FETCH u.role r " +
            "LEFT JOIN FETCH u.departments d " +
            "WHERE (u.isArchived = false OR u.isArchived IS NULL) " +
            "AND (u.status IS NULL OR u.status <> :archivedStatus) " +
            "AND (u.isDeleted = false OR u.isDeleted IS NULL) " +
            "AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) " +
            "AND (u.isSystem = false OR u.isSystem IS NULL) " +
            "AND (r.roleName IS NULL OR UPPER(r.roleName) NOT IN ('SYSTEM_ADMIN', 'ROOTADMIN', 'ROOT_ADMIN', 'ROLE_SYSTEM_ADMIN', 'ROLE_ROOT_ADMIN')) " +
            "AND (u.username IS NULL OR LOWER(u.username) <> 'superadmin') " +
            "AND (:search IS NULL OR :search = '' OR " +
            "     LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(u.systemEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:roleName IS NULL OR :roleName = '' OR :roleName = 'ALL' OR " +
            "     (:roleName = 'OFFICER' AND (r.roleName IS NULL OR UPPER(r.roleName) = 'OFFICER' OR UPPER(r.roleName) = 'USER' OR UPPER(r.roleName) NOT LIKE '%ADMIN%')) OR " +
            "     (:roleName = 'ADMIN' AND (UPPER(r.roleName) = 'ADMIN' OR UPPER(r.roleName) = 'ROLE_ADMIN' OR UPPER(r.roleName) LIKE '%ADMIN%')) OR " +
            "     LOWER(r.roleName) = LOWER(:roleName)) " +
            "AND (:departmentName IS NULL OR :departmentName = '' OR :departmentName = 'ALL' OR LOWER(d.name) = LOWER(:departmentName)) " +
            "AND (:status IS NULL OR " +
            "     (:status = com.barangay.zshared.enumerated.UserStatus.LOCKED AND (u.status = :status OR u.isLocked = true)) OR " +
            "     (:status = com.barangay.zshared.enumerated.UserStatus.ACTIVE AND u.status = :status AND (u.isLocked = false OR u.isLocked IS NULL)) OR " +
            "     u.status = :status) " +
            "ORDER BY u.createdAt DESC")
    List<User> findFilteredUsers(
            @Param("search") String search,
            @Param("roleName") String roleName,
            @Param("departmentName") String departmentName,
            @Param("status") UserStatus status,
            @Param("archivedStatus") UserStatus archivedStatus
    );

    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.person p " +
            "LEFT JOIN FETCH u.role r " +
            "LEFT JOIN FETCH u.departments d " +
            "WHERE (u.isArchived = false OR u.isArchived IS NULL) " +
            "AND (u.status IS NULL OR u.status <> :archivedStatus) " +
            "AND (u.isDeleted = false OR u.isDeleted IS NULL) " +
            "AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) " +
            "AND (u.isSystem = false OR u.isSystem IS NULL) " +
            "AND (u.username IS NULL OR LOWER(u.username) <> 'superadmin') " +
            "AND (r.roleName IS NOT NULL AND UPPER(r.roleName) NOT LIKE '%ADMIN%') " +
            "AND (:search IS NULL OR :search = '' OR " +
            "     LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(u.systemEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:departmentName IS NULL OR :departmentName = '' OR :departmentName = 'ALL' OR LOWER(d.name) = LOWER(:departmentName)) " +
            "AND (:status IS NULL OR " +
            "     (:status = com.barangay.zshared.enumerated.UserStatus.LOCKED AND (u.status = :status OR u.isLocked = true)) OR " +
            "     (:status = com.barangay.zshared.enumerated.UserStatus.ACTIVE AND u.status = :status AND (u.isLocked = false OR u.isLocked IS NULL)) OR " +
            "     u.status = :status) " +
            "ORDER BY u.createdAt DESC")
    List<User> findFilteredSystemUsers(
            @Param("search") String search,
            @Param("departmentName") String departmentName,
            @Param("status") UserStatus status,
            @Param("archivedStatus") UserStatus archivedStatus
    );
}
