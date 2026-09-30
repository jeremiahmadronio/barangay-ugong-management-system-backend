package com.barangay.usermanagement.user.repository;

import com.barangay.usermanagement.user.dto.UserCountRawStats;
import com.barangay.usermanagement.user.dto.UserFilterRequest;
import com.barangay.usermanagement.user.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of custom manual queries for User entity.
 * Filters out SYSTEM_ADMIN and ROOTADMIN accounts from user management table and stats calculations.
 */
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<User> findByEmailWithRoleAndDepartments(@Param("email") String email) {
        String cleanEmail = email != null ? email.trim() : "";
        String jpql = """
                SELECT u FROM User u
                LEFT JOIN FETCH u.role
                LEFT JOIN FETCH u.departments
                WHERE LOWER(u.systemEmail) = LOWER(:email)
                AND (u.isArchived = false OR u.isArchived IS NULL)
                AND (u.status IS NULL OR u.status <> com.barangay.zshared.enumerated.UserStatus.ARCHIVED)
                AND (u.isDeleted = false OR u.isDeleted IS NULL)
                AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL)
                """;
        TypedQuery<User> query = entityManager.createQuery(jpql, User.class);
        query.setParameter("email", cleanEmail);

        return query.getResultStream().findFirst();
    }

    @Override
    public UserCountRawStats getRawUserStats(LocalDateTime startOfCurrentMonth) {
        LocalDateTime startOfPrevMonth = startOfCurrentMonth.minusMonths(1);
        LocalDateTime endOfPrevMonth = startOfCurrentMonth.minusNanos(1);

        String baseFilter = " LEFT JOIN u.role r " +
                "WHERE (u.isArchived = false OR u.isArchived IS NULL) " +
                "AND (u.status IS NULL OR u.status <> com.barangay.zshared.enumerated.UserStatus.ARCHIVED) " +
                "AND (u.isDeleted = false OR u.isDeleted IS NULL) " +
                "AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) " +
                "AND (u.isSystem = false OR u.isSystem IS NULL) " +
                "AND (r.roleName IS NULL OR UPPER(r.roleName) NOT IN ('SYSTEM_ADMIN', 'ROOTADMIN', 'ROOT_ADMIN', 'ROLE_SYSTEM_ADMIN', 'ROLE_ROOT_ADMIN')) " +
                "AND (u.username IS NULL OR LOWER(u.username) <> 'superadmin') ";

        String jpqlTotal = "SELECT COUNT(u) FROM User u " + baseFilter;
        String jpqlTotalPrev = "SELECT COUNT(u) FROM User u " + baseFilter + " AND u.createdAt >= :startOfPrevMonth AND u.createdAt <= :endOfPrevMonth";

        String jpqlActive = "SELECT COUNT(u) FROM User u " + baseFilter + " AND u.status = com.barangay.zshared.enumerated.UserStatus.ACTIVE";
        String jpqlActivePrev = "SELECT COUNT(u) FROM User u " + baseFilter + " AND u.status = com.barangay.zshared.enumerated.UserStatus.ACTIVE AND u.createdAt >= :startOfPrevMonth AND u.createdAt <= :endOfPrevMonth";

        String jpqlLocked = "SELECT COUNT(u) FROM User u " + baseFilter + " AND u.isLocked = true";
        String jpqlLockedPrev = "SELECT COUNT(u) FROM User u " + baseFilter + " AND u.isLocked = true AND u.createdAt >= :startOfPrevMonth AND u.createdAt <= :endOfPrevMonth";

        String jpqlInactive = "SELECT COUNT(u) FROM User u " + baseFilter + " AND u.status = com.barangay.zshared.enumerated.UserStatus.INACTIVE";
        String jpqlInactivePrev = "SELECT COUNT(u) FROM User u " + baseFilter + " AND u.status = com.barangay.zshared.enumerated.UserStatus.INACTIVE AND u.createdAt >= :startOfPrevMonth AND u.createdAt <= :endOfPrevMonth";

        long currentTotal = entityManager.createQuery(jpqlTotal, Long.class).getSingleResult();
        long previousTotal = entityManager.createQuery(jpqlTotalPrev, Long.class)
                .setParameter("startOfPrevMonth", startOfPrevMonth)
                .setParameter("endOfPrevMonth", endOfPrevMonth)
                .getSingleResult();

        long currentActive = entityManager.createQuery(jpqlActive, Long.class).getSingleResult();
        long previousActive = entityManager.createQuery(jpqlActivePrev, Long.class)
                .setParameter("startOfPrevMonth", startOfPrevMonth)
                .setParameter("endOfPrevMonth", endOfPrevMonth)
                .getSingleResult();

        long currentLocked = entityManager.createQuery(jpqlLocked, Long.class).getSingleResult();
        long previousLocked = entityManager.createQuery(jpqlLockedPrev, Long.class)
                .setParameter("startOfPrevMonth", startOfPrevMonth)
                .setParameter("endOfPrevMonth", endOfPrevMonth)
                .getSingleResult();

        long currentInactive = entityManager.createQuery(jpqlInactive, Long.class).getSingleResult();
        long previousInactive = entityManager.createQuery(jpqlInactivePrev, Long.class)
                .setParameter("startOfPrevMonth", startOfPrevMonth)
                .setParameter("endOfPrevMonth", endOfPrevMonth)
                .getSingleResult();

        return new UserCountRawStats(
                currentTotal, previousTotal,
                currentActive, previousActive,
                currentLocked, previousLocked,
                currentInactive, previousInactive
        );
    }

    @Override
    public List<User> findFilteredUsers(UserFilterRequest filter) {
        StringBuilder jpql = new StringBuilder("""
                SELECT DISTINCT u FROM User u
                LEFT JOIN FETCH u.person p
                LEFT JOIN FETCH u.role r
                LEFT JOIN FETCH u.departments d
                WHERE (u.isArchived = false OR u.isArchived IS NULL)
                  AND (u.status IS NULL OR u.status <> com.barangay.zshared.enumerated.UserStatus.ARCHIVED)
                  AND (u.isDeleted = false OR u.isDeleted IS NULL)
                  AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL)
                  AND (u.isSystem = false OR u.isSystem IS NULL)
                  AND (r.roleName IS NULL OR UPPER(r.roleName) NOT IN ('SYSTEM_ADMIN', 'ROOTADMIN', 'ROOT_ADMIN', 'ROLE_SYSTEM_ADMIN', 'ROLE_ROOT_ADMIN'))
                  AND (u.username IS NULL OR LOWER(u.username) <> 'superadmin')
                """);

        Map<String, Object> params = new HashMap<>();

        if (filter != null) {
            // Search filter (matches username, systemEmail, firstName, lastName, fullName)
            if (filter.search() != null && !filter.search().isBlank()) {
                jpql.append("""
                        AND (
                            LOWER(u.username) LIKE LOWER(:search) OR
                            LOWER(u.systemEmail) LIKE LOWER(:search) OR
                            LOWER(p.firstName) LIKE LOWER(:search) OR
                            LOWER(p.lastName) LIKE LOWER(:search) OR
                            LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(:search)
                        )
                        """);
                params.put("search", "%" + filter.search().trim() + "%");
            }

            // Role / Account Type filter
            if (filter.roleName() != null && !filter.roleName().isBlank() && !"ALL".equalsIgnoreCase(filter.roleName())) {
                String role = filter.roleName().trim().toUpperCase();
                if ("USER".equals(role) || "OFFICER".equals(role)) {
                    jpql.append(" AND (r.roleName IS NULL OR UPPER(r.roleName) = 'OFFICER' OR UPPER(r.roleName) = 'USER' OR UPPER(r.roleName) NOT LIKE '%ADMIN%') ");
                } else if ("ADMIN".equals(role)) {
                    jpql.append(" AND (UPPER(r.roleName) = 'ADMIN' OR UPPER(r.roleName) = 'ROLE_ADMIN' OR UPPER(r.roleName) LIKE '%ADMIN%') ");
                } else {
                    jpql.append(" AND LOWER(r.roleName) = LOWER(:roleName) ");
                    params.put("roleName", filter.roleName().trim());
                }
            }

            // Department filter
            if (filter.departmentName() != null && !filter.departmentName().isBlank() && !"ALL".equalsIgnoreCase(filter.departmentName())) {
                jpql.append(" AND (LOWER(d.name) = LOWER(:departmentName) OR LOWER(d.name) LIKE LOWER(:deptPattern)) ");
                params.put("departmentName", filter.departmentName().trim());
                params.put("deptPattern", "%" + filter.departmentName().trim() + "%");
            }

            // Status filter
            if (filter.status() != null) {
                if (filter.status() == com.barangay.zshared.enumerated.UserStatus.LOCKED) {
                    jpql.append(" AND (u.status = :status OR u.isLocked = true) ");
                } else if (filter.status() == com.barangay.zshared.enumerated.UserStatus.ACTIVE) {
                    jpql.append(" AND u.status = :status AND (u.isLocked = false OR u.isLocked IS NULL) ");
                } else {
                    jpql.append(" AND u.status = :status ");
                }
                params.put("status", filter.status());
            }
        }

        jpql.append(" ORDER BY u.createdAt DESC ");

        TypedQuery<User> query = entityManager.createQuery(jpql.toString(), User.class);
        params.forEach(query::setParameter);

        return query.getResultList();
    }
}
