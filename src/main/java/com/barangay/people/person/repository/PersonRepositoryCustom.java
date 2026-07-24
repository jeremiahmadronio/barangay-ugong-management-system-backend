package com.barangay.people.person.repository;

import com.barangay.people.person.model.Person;
import java.util.List;

/**
 * Interface defining custom query signatures for Person entity.
 */
public interface PersonRepositoryCustom {

    /**
     * Searches persons matching the name query who do not currently have an active user account.
     */
    List<Person> findPersonsWithoutUserAccount(String queryStr);
}
