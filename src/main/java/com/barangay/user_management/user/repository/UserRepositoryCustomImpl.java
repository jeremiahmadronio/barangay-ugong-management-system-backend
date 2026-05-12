package com.barangay.user_management.user.repository;

import com.barangay.user_management.user.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
/**
 * This is the actual implementation of our manual queries.
 * We use "EntityManager" to run our own JPQL code directly.
 */
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;


  public Optional<User>findByEmailWithRoleAndDepartments(@Param("email") String email){

      // We manually write the query to fetch Role and Departments
      // linked to the User entity in one trip. 
      String jpql = """
              Select u from User u
              Left join fetch u.role
              Left join fetch u.departments
              Where u.systemEmail = :email
              And u.isArchived = false
              """;
      TypedQuery<User> query = entityManager.createQuery(jpql, User.class);
      query.setParameter("email", email);

      return query.getResultStream().findFirst();

  }
}
