package com.barangay.user_management.user.repository;

import com.barangay.user_management.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * The main access point for all User data.
 * It combines standard Spring Boot tools with our Manual/Complex
 * queries from "UserRepositoryCustom".
 */
@Repository
public interface UserRepository extends JpaRepository<User,Long>,UserRepositoryCustom {

 Optional<User> findBySystemEmail(String systemEmail);

}
