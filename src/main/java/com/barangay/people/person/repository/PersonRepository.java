package com.barangay.people.person.repository;

import com.barangay.people.person.model.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Access point for Person data combining Spring Data JPA with PersonRepositoryCustom.
 */
@Repository
public interface PersonRepository extends JpaRepository<Person, Long>, PersonRepositoryCustom {
}
