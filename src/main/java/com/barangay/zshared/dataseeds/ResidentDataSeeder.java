package com.barangay.zshared.dataseeds;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.barangay.people.person.model.Person;
import com.barangay.people.person.model.Relationship;
import com.barangay.people.person.repository.PersonRepository;
import com.barangay.people.person.repository.RelationshipRepository;
import com.barangay.people.resident.model.Resident;
import com.barangay.people.resident.repository.ResidentRepository;
import com.barangay.zshared.enumerated.ResidentStatus;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ResidentDataSeeder implements CommandLineRunner {

    private final PersonRepository personRepository;
    private final ResidentRepository residentRepository;
    private final RelationshipRepository relationshipRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (residentRepository.count() >= 100) {
            System.out.println("Residents already seeded (" + residentRepository.count() + " records). Skipping...");
            return;
        }

        seedResidents(100);
    }

    private void seedResidents(int count) {
        String[] firstNamesMale = {
                "Juan", "Jose", "Ricardo", "Roberto", "Fernando", "Francisco", "Antonio", "Jaime",
                "Benjamin", "Rodolfo", "Eduardo", "Manuel", "Ramon", "Ernesto", "Alfredo", "Domingo",
                "Renato", "Rodrigo", "Danilo", "Virgilio", "Armando", "Leonidas", "Crisanto", "Marcelo",
                "Conrado", "Wilfredo", "Rolando", "Edmundo", "Teodoro", "Gervacio", "Nestor", "Efren",
                "Gregorio", "Isidro", "Lorenzo", "Maximo", "Narciso", "Onofre", "Pascual", "Quintin",
                "Silverio", "Tomas", "Urbano", "Vicente", "Wenceslao", "Bernardo", "Cesar", "Diego",
                "Enrique", "Felipe", "Gilberto", "Hernan", "Ignacio", "Julio", "Mariano", "Oscar"
        };

        String[] firstNamesFemale = {
                "Maria", "Elena", "Gloria", "Teresa", "Imelda", "Lourdes", "Corazon", "Remedios",
                "Carmelita", "Dolores", "Rosario", "Ligaya", "Perla", "Nenita", "Erlinda", "Felicitas",
                "Herminia", "Irene", "Josefina", "Lorena", "Natividad", "Ofelia", "Salome", "Teresita",
                "Ursula", "Yolanda", "Zenaida", "Esperanza", "Fe", "Gertrudes", "Isabelita",
                "Leonora", "Noemi", "Ophelia", "Rosalinda", "Thelma", "Vilma", "Adelaida", "Belen",
                "Cecilia", "Elisa", "Florencia", "Gina", "Hilda", "Ines", "Juanita", "Luz"
        };

        String[] lastNames = {
                "Dela Cruz", "Garcia", "Reyes", "Ramos", "Mendoza", "Santos", "Flores", "Gonzales",
                "Bautista", "Villanueva", "Fernandez", "Cruz", "Lopez", "Castillo", "Gomez", "Pineda",
                "Madronio", "De Leon", "Mercado", "Rivera", "Torres", "Aquino", "Diaz", "Marquez",
                "Soriano", "Santiago", "Espiritu", "Navarro", "Aguilar", "Pascual", "Morales", "Perez",
                "Salazar", "Hidalgo", "Velasquez", "Ocampo", "Manalo", "Tolentino", "Magno", "Roxas"
        };

        String[] educationalAttainments = {
                "Elementary Graduate", "High School Graduate", "Vocational/Technical",
                "College Graduate", "Post Graduate"
        };
        String[] occupations = {
                "Employee", "Self-employed", "Vendor", "Driver", "Carpenter", "Teacher",
                "Nurse", "Security Guard", "Mechanic", "Housewife", "Laborer",
                "Government Employee", "Electrician", "Plumber", "Cook", "Salesperson",
                "Tricycle Driver", "Construction Worker", "Technician", "Accountant"
        };
        String[] streets = {
                "Purok 1", "Purok 2", "Purok 3", "Purok 4", "Purok 5", "Purok 6",
                "Purok 7", "Purok 8", "Sitio Malinis", "Sitio Bagong Buhay",
                "Sitio Maliwanag", "Sitio Kaunlaran"
        };

        Random random = new Random();
        System.out.println("Seeding " + count + " Filipino Residents in Brgy. Ugong with Associated With (Family Relationships)...");

        List<Person> createdPersons = new ArrayList<>();
        int residentCounter = 0;
        int familyIndex = 1;

        while (residentCounter < count) {
            String familyLastName = lastNames[random.nextInt(lastNames.length)];
            String householdNum = "2026-HH-" + String.format("%04d", familyIndex++);
            String street = streets[random.nextInt(streets.length)];
            String completeAddress = street + ", Brgy. Ugong, Valenzuela City, Metro Manila";

            // Family size between 2 and 4 members
            int familySize = Math.min(2 + random.nextInt(3), count - residentCounter);
            List<Person> familyMembers = new ArrayList<>();

            for (int f = 0; f < familySize; f++) {
                residentCounter++;
                boolean isMale = (f % 2 == 0);
                String firstName = isMale ? firstNamesMale[random.nextInt(firstNamesMale.length)]
                                         : firstNamesFemale[random.nextInt(firstNamesFemale.length)];
                String middleName = lastNames[random.nextInt(lastNames.length)];

                Person person = new Person();
                person.setFirstName(firstName);
                person.setLastName(familyLastName);
                person.setMiddleName(middleName);
                person.setGender(isMale ? "Male" : "Female");

                int age;
                boolean isHead = (f == 0);
                if (isHead) {
                    age = 35 + random.nextInt(35); // Head: age 35 - 70
                    person.setCivilStatus("Married");
                } else if (f == 1 && familySize > 1) {
                    age = Math.max(18, familyMembers.get(0).getAge() + (random.nextInt(7) - 3)); // Spouse
                    person.setCivilStatus("Married");
                } else {
                    age = 5 + random.nextInt(20); // Child
                    person.setCivilStatus("Single");
                }

                person.setAge((short) age);
                person.setBirthDate(LocalDate.now().minusYears(age).minusDays(random.nextInt(365)));
                person.setContactNumber("09" + (100000000 + random.nextInt(900000000)));
                person.setCompleteAddress(completeAddress);
                person.setIsResident(true);
                person.setEmail(firstName.toLowerCase().replaceAll("\\s+", "") + residentCounter + "@gmail.com");
                person.setOccupation(age < 18 ? "Student" : (age >= 65 ? "Retired" : occupations[random.nextInt(occupations.length)]));

                Person savedPerson = personRepository.save(person);

                Resident resident = new Resident();
                resident.setPerson(savedPerson);
                resident.setBarangayIdNumber("2026-BID-" + String.format("%04d", residentCounter));
                resident.setHouseholdNumber(householdNum);
                resident.setIsHeadOfFamily(isHead);
                resident.setCitizenship("Filipino");
                resident.setDateOfResidency(LocalDate.now().minusYears(random.nextInt(15)).minusDays(random.nextInt(365)));
                resident.setEducationalAttainment(educationalAttainments[random.nextInt(educationalAttainments.length)]);
                resident.setStatus(ResidentStatus.ACTIVE);

                residentRepository.save(resident);
                savedPerson.setResident(resident);
                familyMembers.add(savedPerson);
                createdPersons.add(savedPerson);
            }

            // Create Family Relationships (Associated With)
            if (familyMembers.size() >= 2) {
                Person head = familyMembers.get(0);
                for (int m = 1; m < familyMembers.size(); m++) {
                    Person member = familyMembers.get(m);
                    String relType;
                    if (m == 1 && Math.abs(head.getAge() - member.getAge()) < 15) {
                        relType = "Spouse";
                    } else if (member.getAge() < head.getAge()) {
                        relType = "Child";
                    } else {
                        relType = "Sibling";
                    }

                    // Link Head -> Member
                    Relationship r1 = new Relationship();
                    r1.setPerson(head);
                    r1.setRelative(member);
                    r1.setRelationshipType(relType);
                    relationshipRepository.save(r1);

                    // Link Member -> Head (Inverse)
                    String inverseType = relType.equals("Child") ? "Parent" : relType;
                    Relationship r2 = new Relationship();
                    r2.setPerson(member);
                    r2.setRelative(head);
                    r2.setRelationshipType(inverseType);
                    relationshipRepository.save(r2);
                }
            }
        }

        System.out.println("Successfully seeded " + createdPersons.size() + " residents with family relationships (Associated With) in Serbisyoko-Web.");
    }
}
