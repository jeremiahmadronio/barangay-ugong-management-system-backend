package com.barangay.zshared.dataseeds;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
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

import java.time.LocalDateTime;

@Component
@Order(5)
@RequiredArgsConstructor
public class ResidentDataSeeder implements CommandLineRunner {

    private final PersonRepository personRepository;
    private final ResidentRepository residentRepository;
    private final RelationshipRepository relationshipRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        if (residentRepository.count() >= 100) {
            System.out.println("Residents already seeded (" + residentRepository.count() + " records). Normalizing resident addresses/relationships and distributing dates...");
            normalizeExistingResidentData();
            distributeHistoricalDatesIfSingleDay();
            return;
        }

        seedResidents(100);
        normalizeExistingResidentData();
        distributeHistoricalDatesIfSingleDay();
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
                "Elementary", "High School", "Vocational",
                "College", "Post Graduate"
        };
        String[] occupations = {
                "Employee", "Self-employed", "Vendor", "Driver", "Carpenter", "Teacher",
                "Nurse", "Security Guard", "Mechanic", "Housewife", "Laborer",
                "Government Employee", "Electrician", "Plumber", "Cook", "Salesperson",
                "Tricycle Driver", "Construction Worker", "Technician", "Accountant"
        };
        String[] houseNumbers = {
                "12A", "Unit 3B", "55-S", "8B", "24-A", "143", "77", "9", "305", "18",
                "42", "88", "15-C", "230", "64", "99", "120", "33", "5B", "71", "Building 4"
        };
        String[] streetNames = {
                "F. Legaspi Street", "Francisco Street", "M. De Leon St.", "Mabini St.",
                "Rizal St.", "P. Valenzuela St.", "San Diego St.", "Del Pilar St.",
                "Servando St.", "Kagitingan St.", "Katipunan St.", "Bonifacio St."
        };
        String[] puroks = {
                "Purok 1", "Purok 2", "Purok 3", "Purok 4", "Purok 5", "Purok 6",
                "Purok 7", "Purok 8", "Purok 3 / Phase 2", "Sitio Malinis",
                "Sitio Bagong Buhay", "Sitio Maliwanag", "Sitio Kaunlaran"
        };

        Random random = new Random();
        System.out.println("Seeding " + count + " Filipino Residents in Brgy. Ugong with Associated With (Family Relationships)...");

        List<Person> createdPersons = new ArrayList<>();
        int residentCounter = 0;
        int familyIndex = 1;

        while (residentCounter < count) {
            String familyLastName = lastNames[random.nextInt(lastNames.length)];
            String householdNum = "2026-HH-" + String.format("%04d", familyIndex++);
            String houseNo = houseNumbers[random.nextInt(houseNumbers.length)];
            String street = streetNames[random.nextInt(streetNames.length)];
            String purok = puroks[random.nextInt(puroks.length)];
            String completeAddress = String.format("%s, %s, %s, Ugong, Valenzuela City, Metro Manila", houseNo, street, purok);

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
                resident.setPrecinctNumber(String.format("0%03d%s", 100 + (familyIndex % 25), (char)('A' + (familyIndex % 3))));
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
                        relType = "Female".equalsIgnoreCase(member.getGender()) ? "Daughter" : "Son";
                    } else {
                        relType = "Female".equalsIgnoreCase(member.getGender()) ? "Sister" : "Brother";
                    }

                    // Link Head -> Member
                    Relationship r1 = new Relationship();
                    r1.setPerson(head);
                    r1.setRelative(member);
                    r1.setRelationshipType(relType);
                    relationshipRepository.save(r1);

                    // Link Member -> Head (Inverse)
                    String inverseType;
                    if ("Daughter".equals(relType) || "Son".equals(relType) || "Child".equals(relType)) {
                        inverseType = "Female".equalsIgnoreCase(head.getGender()) ? "Mother" : "Father";
                    } else if ("Sister".equals(relType) || "Brother".equals(relType) || "Sibling".equals(relType)) {
                        inverseType = "Female".equalsIgnoreCase(head.getGender()) ? "Sister" : "Brother";
                    } else {
                        inverseType = "Spouse";
                    }
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

    private void distributeHistoricalDatesIfSingleDay() {
        try {
            List<Resident> residents = residentRepository.findAll();
            if (residents.isEmpty()) return;

            LocalDate now = LocalDate.now();
            Random rand = new Random(42);

            for (int i = 0; i < residents.size(); i++) {
                Resident r = residents.get(i);
                // Distribute across last 6 months (0 to 170 days ago)
                int daysAgo = (int) ((i / (double) residents.size()) * 170) + rand.nextInt(5);
                LocalDateTime histDate = now.minusDays(daysAgo).atTime(8 + rand.nextInt(9), rand.nextInt(60), rand.nextInt(60));

                jdbcTemplate.update("UPDATE resident SET created_at = ?, updated_at = ? WHERE resident_id = ?",
                        histDate, histDate, r.getResidentId());
                if (r.getPerson() != null && r.getPerson().getId() != null) {
                    jdbcTemplate.update("UPDATE person SET created_at = ?, updated_at = ? WHERE id = ?",
                            histDate, histDate, r.getPerson().getId());
                }
            }
            System.out.println("Distributed " + residents.size() + " resident creation dates across the past 6 months for realistic monthly registration trend analytics.");
        } catch (Exception e) {
            System.err.println("Could not distribute resident historical dates: " + e.getMessage());
        }
    }

    private void normalizeExistingResidentData() {
        try {
            List<Resident> residents = residentRepository.findAll();
            if (residents.isEmpty()) return;

            String[] houseNumbers = {
                    "12A", "Unit 3B", "55-S", "8B", "24-A", "143", "77", "9", "305", "18",
                    "42", "88", "15-C", "230", "64", "99", "120", "33", "5B", "71", "Building 4"
            };
            String[] streetNames = {
                    "F. Legaspi Street", "Francisco Street", "M. De Leon St.", "Mabini St.",
                    "Rizal St.", "P. Valenzuela St.", "San Diego St.", "Del Pilar St.",
                    "Servando St.", "Kagitingan St.", "Katipunan St.", "Bonifacio St."
            };
            String[] puroks = {
                    "Purok 1", "Purok 2", "Purok 3", "Purok 4", "Purok 5", "Purok 6",
                    "Purok 7", "Purok 8", "Purok 3 / Phase 2", "Sitio Malinis",
                    "Sitio Bagong Buhay", "Sitio Maliwanag", "Sitio Kaunlaran"
            };

            Random random = new Random(42);
            java.util.Map<String, String> householdAddressMap = new java.util.HashMap<>();

            for (Resident r : residents) {
                Person p = r.getPerson();
                if (p == null) continue;

                String addr = p.getCompleteAddress();
                String hh = r.getHouseholdNumber() != null && !r.getHouseholdNumber().isBlank()
                        ? r.getHouseholdNumber()
                        : "HH-" + r.getResidentId();

                boolean needsAddressFix = false;
                if (addr == null || addr.isBlank()) {
                    needsAddressFix = true;
                } else {
                    String[] parts = addr.split(",");
                    if (parts.length < 4) {
                        needsAddressFix = true;
                    }
                }

                if (needsAddressFix) {
                    if (!householdAddressMap.containsKey(hh)) {
                        String existingPurok = null;
                        if (addr != null && !addr.isBlank()) {
                            for (String candidatePurok : puroks) {
                                if (addr.toLowerCase().contains(candidatePurok.toLowerCase())) {
                                    existingPurok = candidatePurok;
                                    break;
                                }
                            }
                        }
                        String finalPurok = existingPurok != null ? existingPurok : puroks[random.nextInt(puroks.length)];
                        String houseNo = houseNumbers[random.nextInt(houseNumbers.length)];
                        String street = streetNames[random.nextInt(streetNames.length)];
                        String fullAddress = String.format("%s, %s, %s, Ugong, Valenzuela City, Metro Manila", houseNo, street, finalPurok);
                        householdAddressMap.put(hh, fullAddress);
                    }

                    String assignedAddress = householdAddressMap.get(hh);
                    p.setCompleteAddress(assignedAddress);
                    personRepository.save(p);
                }
            }

            // Normalize relationships (Child -> Son/Daughter, Parent -> Father/Mother, Sibling -> Brother/Sister)
            List<Relationship> relationships = relationshipRepository.findAll();
            for (Relationship rel : relationships) {
                String type = rel.getRelationshipType();
                if (type == null) continue;
                Person relative = rel.getRelative();
                if (relative == null) continue;
                boolean isFemale = "Female".equalsIgnoreCase(relative.getGender());

                if ("Child".equalsIgnoreCase(type)) {
                    rel.setRelationshipType(isFemale ? "Daughter" : "Son");
                    relationshipRepository.save(rel);
                } else if ("Parent".equalsIgnoreCase(type)) {
                    rel.setRelationshipType(isFemale ? "Mother" : "Father");
                    relationshipRepository.save(rel);
                } else if ("Sibling".equalsIgnoreCase(type)) {
                    rel.setRelationshipType(isFemale ? "Sister" : "Brother");
                    relationshipRepository.save(rel);
                }
            }

            // Normalize educational attainment
            for (Resident r : residents) {
                String edu = r.getEducationalAttainment();
                if (edu == null || edu.isBlank()) continue;
                String normalizedEdu = edu;
                String lower = edu.toLowerCase();
                if (lower.contains("college") || lower.contains("bachelor")) normalizedEdu = "College";
                else if (lower.contains("elementary")) normalizedEdu = "Elementary";
                else if (lower.contains("high school")) normalizedEdu = "High School";
                else if (lower.contains("vocational") || lower.contains("technical")) normalizedEdu = "Vocational";
                else if (lower.contains("post")) normalizedEdu = "Post Graduate";
                else if (lower.contains("no formal")) normalizedEdu = "No Formal Education";

                if (!normalizedEdu.equals(edu)) {
                    r.setEducationalAttainment(normalizedEdu);
                    residentRepository.save(r);
                }
            }

            // Normalize precinct numbers for existing records
            for (Resident r : residents) {
                if (r.getPrecinctNumber() == null || r.getPrecinctNumber().isBlank()) {
                    String hh = r.getHouseholdNumber() != null ? r.getHouseholdNumber() : "HH" + r.getResidentId();
                    int hash = Math.abs(hh.hashCode());
                    String precinct = String.format("0%03d%s", 100 + (hash % 25), (char)('A' + (hash % 3)));
                    r.setPrecinctNumber(precinct);
                    residentRepository.save(r);
                }
            }

            System.out.println("Normalized resident addresses, family relationships, educational attainment, and precinct numbers in database.");
        } catch (Exception e) {
            System.err.println("Could not normalize resident data: " + e.getMessage());
        }
    }
}
