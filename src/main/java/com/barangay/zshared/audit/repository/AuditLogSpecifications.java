package com.barangay.zshared.audit.repository;

import com.barangay.people.person.model.Person;
import com.barangay.usermanagement.role.model.Role;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.audit.model.AuditLogs;
import com.barangay.zshared.enumerated.Severity;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * JPA Specifications for dynamic filtering and searching of {@link AuditLogs}.
 */
public final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    /**
     * Filters audit logs matching the given search query across user, role, and module fields.
     */
    public static Specification<AuditLogs> search(String rawSearch) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(rawSearch)) {
                return cb.conjunction();
            }

            query.distinct(true);

            Join<AuditLogs, User> userJoin = root.join("user", JoinType.LEFT);
            Join<User, Person> personJoin = userJoin.join("person", JoinType.LEFT);
            Join<User, Role> roleJoin = userJoin.join("role", JoinType.LEFT);

            String search = "%" + rawSearch.trim().toLowerCase() + "%";
            Expression<String> username = cb.lower(cb.coalesce(userJoin.get("username"), ""));
            Expression<String> firstName = cb.lower(cb.coalesce(personJoin.get("firstName"), ""));
            Expression<String> lastName = cb.lower(cb.coalesce(personJoin.get("lastName"), ""));
            Expression<String> fullName = cb.lower(
                    cb.concat(
                            cb.concat(cb.coalesce(personJoin.get("firstName"), ""), " "),
                            cb.coalesce(personJoin.get("lastName"), "")
                    )
            );
            Expression<String> roleName = cb.lower(cb.coalesce(roleJoin.get("roleName"), ""));
            Expression<String> module = cb.lower(root.get("module").as(String.class));

            return cb.or(
                    cb.like(username, search),
                    cb.like(firstName, search),
                    cb.like(lastName, search),
                    cb.like(fullName, search),
                    cb.like(roleName, search),
                    cb.like(module, search)
            );
        };
    }

    /**
     * Filters audit logs by severity level.
     */
    public static Specification<AuditLogs> hasSeverity(Severity severity) {
        return (root, query, cb) ->
                severity == null ? cb.conjunction() : cb.equal(root.get("severity"), severity);
    }

    /**
     * Filters audit logs created on or after the specified timestamp.
     */
    public static Specification<AuditLogs> createdOnOrAfter(LocalDateTime fromDateTime) {
        return (root, query, cb) ->
                fromDateTime == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), fromDateTime);
    }

    /**
     * Filters audit logs created on or before the specified timestamp.
     */
    public static Specification<AuditLogs> createdOnOrBefore(LocalDateTime toDateTime) {
        return (root, query, cb) ->
                toDateTime == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("createdAt"), toDateTime);
    }

    /**
     * Excludes view-only audit actions (VIEW_*, LIST_*, GET_*).
     */
    public static Specification<AuditLogs> excludeViewActions() {
        return (root, query, cb) -> {
            Expression<String> action = cb.upper(root.get("actionTaken").as(String.class));
            return cb.and(
                    cb.notLike(action, "VIEW%"),
                    cb.notLike(action, "LIST%"),
                    cb.notLike(action, "GET%")
            );
        };
    }
}
