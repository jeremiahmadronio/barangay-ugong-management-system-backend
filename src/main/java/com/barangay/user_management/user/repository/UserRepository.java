package com.barangay.user_management.user.repository;

import com.barangay.user_management.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {

    @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.role
        LEFT JOIN FETCH u.departments
        WHERE u.systemEmail = :email
          AND u.isArchived = false
        """)
    Optional<User> findByEmailWithRoleAndDepartments(
            @Param("email") String email
    );
}
