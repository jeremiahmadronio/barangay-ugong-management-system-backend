package com.barangay.people.person.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.barangay.people.person.dto.PersonOptionResponse;
import com.barangay.people.person.model.Person;
import com.barangay.people.person.repository.PersonRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("PersonService BDD Unit Tests")
class PersonServiceTest {

    @Mock
    private PersonRepository personRepository;

    @InjectMocks
    private PersonService personService;

    @Nested
    @DisplayName("searchAvailablePersonsForUserCreation Method Tests")
    class SearchAvailablePersonsTests {

        @Test
        @DisplayName("shouldReturnAvailablePersons_whenMatchingQueryProvided")
        void shouldReturnAvailablePersons_whenMatchingQueryProvided() {
            // Given
            String query = "Juan";
            Person person = new Person();
            person.setId(1L);
            person.setFirstName("Juan");
            person.setLastName("Dela Cruz");
            person.setMiddleName("Reyes");
            person.setContactNumber("09123456789");
            person.setCompleteAddress("Zone 1");

            when(personRepository.findPersonsWithoutUserAccount(query)).thenReturn(List.of(person));

            // When
            List<PersonOptionResponse> result = personService.searchAvailablePersonsForUserCreation(query);

            // Then
            assertThat(result).isNotNull().hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(1L);
            assertThat(result.get(0).fullName()).isEqualTo("Juan Reyes Dela Cruz");
            assertThat(result.get(0).contactNumber()).isEqualTo("09123456789");
            verify(personRepository, times(1)).findPersonsWithoutUserAccount(query);
        }

        @Test
        @DisplayName("shouldReturnEmptyList_whenNoPersonsMatchQuery")
        void shouldReturnEmptyList_whenNoPersonsMatchQuery() {
            // Given
            String query = "NonExistent";
            when(personRepository.findPersonsWithoutUserAccount(query)).thenReturn(Collections.emptyList());

            // When
            List<PersonOptionResponse> result = personService.searchAvailablePersonsForUserCreation(query);

            // Then
            assertThat(result).isNotNull().isEmpty();
            verify(personRepository, times(1)).findPersonsWithoutUserAccount(query);
        }
    }
}
