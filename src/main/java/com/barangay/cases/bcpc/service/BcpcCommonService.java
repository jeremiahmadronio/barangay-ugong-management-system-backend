package com.barangay.cases.bcpc.service;

import com.barangay.cases.bcpc.dto.BcpcPersonDetailDto;
import com.barangay.cases.bcpc.dto.BcpcPersonInputDto;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.model.CaseEvidence;
import com.barangay.cases.shared.model.NarrativeStatement;
import com.barangay.cases.shared.repository.CaseEvidenceRepository;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.people.officer.model.Officer;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.person.repository.PersonRepository;
import com.barangay.people.resident.model.Resident;
import com.barangay.people.resident.repository.ResidentRepository;
import com.barangay.people.witness.model.Witness;
import com.barangay.people.witness.repository.WitnessRepository;
import com.barangay.zshared.dto.EvidenceCreateDto;
import com.barangay.zshared.dto.NarrativeCreateDto;
import com.barangay.zshared.dto.WitnessCreateDto;
import com.barangay.zshared.mapper.CaseEvidenceMapper;
import com.barangay.zshared.mapper.NarrativeStatementMapper;
import com.barangay.zshared.mapper.PersonMapper;
import com.barangay.zshared.mapper.WitnessMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Common shared utility service for BCPC case modules (CAR, CICL, Custody, etc.).
 * Handles person resolution, case numbering, selective file and witness updates, and common entity conversions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BcpcCommonService {

    private final CaseRepository caseRepository;
    private final PersonRepository personRepository;
    private final ResidentRepository residentRepository;
    private final OfficerRepository officerRepository;
    private final WitnessRepository witnessRepository;
    private final CaseEvidenceRepository caseEvidenceRepository;

    private final PersonMapper personMapper;
    private final WitnessMapper witnessMapper;
    private final CaseEvidenceMapper caseEvidenceMapper;
    private final NarrativeStatementMapper narrativeStatementMapper;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a unique case number in the format: 2026-BCPC-XXXX
     */
    public String generateUniqueBcpcCaseNumber() {
        int year = LocalDate.now().getYear();
        String candidate;
        int attempts = 0;
        do {
            int randomPart = 1000 + RANDOM.nextInt(9000);
            candidate = String.format("%d-BCPC-%04d", year, randomPart);
            attempts++;
            if (attempts > 50) {
                candidate = String.format("%d-BCPC-%d", year, System.currentTimeMillis() % 100000);
                break;
            }
        } while (caseRepository.existsByCaseNumber(candidate));
        return candidate;
    }

    /**
     * Resolves an officer by full name search.
     */
    public Optional<Officer> resolveOfficerByName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        return officerRepository.findAll().stream()
                .filter(o -> o.getPerson() != null && personMapper.buildFullName(o.getPerson()).equalsIgnoreCase(name.trim()))
                .findFirst();
    }

    /**
     * Resolves a Person by residentId, personId, or creates a new Person entity if manual.
     */
    public Person resolveOrCreatePerson(BcpcPersonInputDto dto) {
        if (dto == null) return null;

        if (dto.residentId() != null) {
            Optional<Resident> residentOpt = residentRepository.findById(dto.residentId());
            if (residentOpt.isPresent() && residentOpt.get().getPerson() != null) {
                Person person = residentOpt.get().getPerson();
                boolean updated = false;
                if ((person.getAge() == null || person.getAge() == 0) && person.getBirthDate() != null) {
                    person.setAge((short) person.getCalculatedAge());
                    updated = true;
                } else if ((person.getAge() == null || person.getAge() == 0) && dto.age() != null) {
                    person.setAge(dto.age());
                    updated = true;
                }
                if (person.getEmail() == null || person.getEmail().isBlank()) {
                    if (dto.email() != null && !dto.email().isBlank()) {
                        person.setEmail(dto.email().trim());
                        updated = true;
                    } else if (person.getDisplayEmail() != null) {
                        person.setEmail(person.getDisplayEmail());
                        updated = true;
                    }
                }
                if (dto.contactNumber() != null && !dto.contactNumber().isBlank()) {
                    person.setContactNumber(dto.contactNumber().trim());
                    updated = true;
                }
                if (updated) {
                    person = personRepository.save(person);
                }
                return person;
            }
        }

        if (dto.personId() != null) {
            Optional<Person> personOpt = personRepository.findById(dto.personId());
            if (personOpt.isPresent()) {
                Person person = personOpt.get();
                boolean updated = false;
                if ((person.getAge() == null || person.getAge() == 0) && person.getBirthDate() != null) {
                    person.setAge((short) person.getCalculatedAge());
                    updated = true;
                } else if ((person.getAge() == null || person.getAge() == 0) && dto.age() != null) {
                    person.setAge(dto.age());
                    updated = true;
                }
                if (person.getEmail() == null || person.getEmail().isBlank()) {
                    if (dto.email() != null && !dto.email().isBlank()) {
                        person.setEmail(dto.email().trim());
                        updated = true;
                    } else if (person.getDisplayEmail() != null) {
                        person.setEmail(person.getDisplayEmail());
                        updated = true;
                    }
                }
                if (dto.contactNumber() != null && !dto.contactNumber().isBlank()) {
                    person.setContactNumber(dto.contactNumber().trim());
                    updated = true;
                }
                if (updated) {
                    person = personRepository.save(person);
                }
                return person;
            }
        }

        // Manual Input: create new Person
        Person person = new Person();
        person.setFirstName(dto.firstName() != null ? dto.firstName().trim() : null);
        person.setLastName(dto.lastName() != null ? dto.lastName().trim() : null);
        person.setMiddleName(dto.middleName() != null && !dto.middleName().isBlank() ? dto.middleName().trim() : null);
        person.setSuffix(dto.suffix() != null && !dto.suffix().isBlank() ? dto.suffix().trim() : null);
        person.setContactNumber(dto.contactNumber());
        person.setCompleteAddress(dto.completeAddress());
        Short calculatedAge = dto.age();
        if (calculatedAge == null && dto.birthDate() != null) {
            calculatedAge = (short) Period.between(dto.birthDate(), LocalDate.now()).getYears();
        }
        person.setAge(calculatedAge);
        person.setGender(dto.gender());
        person.setBirthDate(dto.birthDate());
        person.setCivilStatus(dto.civilStatus());
        person.setEmail(dto.email());
        person.setIsResident(false);

        return personRepository.save(person);
    }

    /**
     * Converts a Person entity to BcpcPersonDetailDto.
     */
    public BcpcPersonDetailDto toPersonDetailDto(Person p) {
        if (p == null) return null;
        Short age = p.getAge();
        if ((age == null || age == 0) && p.getBirthDate() != null) {
            age = (short) p.getCalculatedAge();
        }
        String email = p.getEmail();
        if (email == null || email.isBlank()) {
            email = p.getDisplayEmail();
        }
        return new BcpcPersonDetailDto(
                p.getId(),
                p.getFirstName(),
                p.getMiddleName(),
                p.getLastName(),
                personMapper.buildFullName(p),
                p.getSuffix(),
                p.getContactNumber(),
                p.getCompleteAddress(),
                age,
                p.getGender(),
                p.getBirthDate(),
                p.getCivilStatus(),
                email,
                p.getIsResident()
        );
    }

    /**
     * Updates Narrative Statement in place, preserving existing file bytes if not re-uploaded.
     */
    public void updateNarrative(Case aCase, NarrativeCreateDto narrativeDto) {
        if (narrativeDto == null) return;
        if (aCase.getNarrativeStatement() == null) {
            aCase.setNarrativeStatement(narrativeStatementMapper.fromCreateDto(narrativeDto));
        } else {
            NarrativeStatement narr = aCase.getNarrativeStatement();
            if (narrativeDto.statementSummary() != null) {
                narr.setStatementSummary(narrativeDto.statementSummary());
            }
            if (narrativeDto.fileName() != null && !narrativeDto.fileName().isBlank()) {
                narr.setFileName(narrativeDto.fileName());
            }
            if (narrativeDto.fileData() != null && narrativeDto.fileData().length > 0) {
                narr.setFileData(narrativeDto.fileData());
            }
        }
    }

    /**
     * Selectively updates Case Evidence items, preserving existing files and removing unselected items.
     */
    public void updateEvidences(Case aCase, List<EvidenceCreateDto> evidences) {
        if (evidences == null) return;

        List<CaseEvidence> existingEvidences = caseEvidenceRepository.findByCasesId(aCase.getId());
        Map<String, CaseEvidence> existingEvMapByName = new HashMap<>();
        Map<Long, CaseEvidence> existingEvMapById = new HashMap<>();
        for (CaseEvidence ce : existingEvidences) {
            if (ce.getId() != null) {
                existingEvMapById.put(ce.getId(), ce);
            }
            if (ce.getType() != null && ce.getType().getName() != null) {
                existingEvMapByName.put(ce.getType().getName().trim().toLowerCase(), ce);
            }
        }

        List<CaseEvidence> updatedEvidences = new ArrayList<>();
        for (EvidenceCreateDto eDto : evidences) {
            String typeName = (eDto.evidenceTypeName() != null) ? eDto.evidenceTypeName().trim() : "General Evidence";
            CaseEvidence targetEvidence = null;
            if (eDto.id() != null && existingEvMapById.containsKey(eDto.id())) {
                targetEvidence = existingEvMapById.get(eDto.id());
            } else if (existingEvMapByName.containsKey(typeName.toLowerCase())) {
                targetEvidence = existingEvMapByName.get(typeName.toLowerCase());
            }

            if (targetEvidence != null) {
                if (eDto.files() != null && eDto.files().length > 0) {
                    targetEvidence.setFiles(eDto.files());
                    if (eDto.fileName() != null && !eDto.fileName().isBlank()) {
                        targetEvidence.setFileName(eDto.fileName().trim());
                    }
                }
                if (eDto.description() != null) {
                    targetEvidence.setDescription(eDto.description());
                }
                targetEvidence = caseEvidenceRepository.save(targetEvidence);
                updatedEvidences.add(targetEvidence);
            } else {
                CaseEvidence newEv = caseEvidenceMapper.fromCreateDto(eDto, aCase);
                if (newEv != null) {
                    newEv = caseEvidenceRepository.save(newEv);
                    updatedEvidences.add(newEv);
                }
            }
        }

        for (CaseEvidence ce : existingEvidences) {
            if (!updatedEvidences.contains(ce)) {
                caseEvidenceRepository.delete(ce);
            }
        }
    }

    /**
     * Selectively updates Witnesses in place, preserving existing testimonies and adding newly submitted ones.
     */
    public void updateWitnesses(Case aCase, List<WitnessCreateDto> witnesses) {
        if (witnesses == null) return;

        List<Witness> currentWitnessList = (aCase.getWitnesses() != null && !aCase.getWitnesses().isEmpty())
                ? new ArrayList<>(aCase.getWitnesses())
                : witnessRepository.findByCaseId(aCase.getId());

        Map<Long, Witness> existingWitnessById = new HashMap<>();
        Map<Integer, Witness> existingWitnessByIndex = new HashMap<>();
        int idx = 0;
        for (Witness w : currentWitnessList) {
            if (w.getId() != null) {
                existingWitnessById.put(w.getId(), w);
            }
            existingWitnessByIndex.put(idx++, w);
        }

        List<Witness> updatedWitnessList = new ArrayList<>();
        int inputIdx = 0;
        for (WitnessCreateDto wDto : witnesses) {
            Witness targetWitness = null;
            if (wDto.id() != null && existingWitnessById.containsKey(wDto.id())) {
                targetWitness = existingWitnessById.get(wDto.id());
            } else if (existingWitnessByIndex.containsKey(inputIdx)) {
                targetWitness = existingWitnessByIndex.get(inputIdx);
            }

            if (targetWitness != null) {
                Person wPerson = targetWitness.getPerson();
                if (wPerson == null && wDto.personId() != null) {
                    wPerson = personRepository.findById(wDto.personId()).orElse(null);
                }
                if (wPerson == null && wDto.firstName() != null && !wDto.firstName().isBlank()) {
                    wPerson = new Person();
                    wPerson.setFirstName(wDto.firstName().trim());
                    wPerson.setLastName(wDto.lastName() != null ? wDto.lastName().trim() : "");
                    wPerson.setIsResident(false);
                    wPerson = personRepository.save(wPerson);
                }
                if (wPerson != null) {
                    if (wDto.firstName() != null && !wDto.firstName().isBlank()) {
                        wPerson.setFirstName(wDto.firstName().trim());
                    }
                    if (wDto.lastName() != null) {
                        wPerson.setLastName(wDto.lastName().trim());
                    }
                    if (wDto.contactNumber() != null) {
                        wPerson.setContactNumber(wDto.contactNumber());
                    }
                    if (wDto.completeAddress() != null) {
                        wPerson.setCompleteAddress(wDto.completeAddress());
                    }
                    personRepository.save(wPerson);
                    targetWitness.setPerson(wPerson);
                }

                if (wDto.testimonyText() != null) {
                    targetWitness.setTestimony(wDto.testimonyText());
                }
                if (wDto.testimonyFileName() != null) {
                    targetWitness.setTestimonyFileName(wDto.testimonyFileName());
                }
                if (wDto.testimonyFile() != null && wDto.testimonyFile().length > 0) {
                    targetWitness.setTestimonyFile(wDto.testimonyFile());
                }

                targetWitness = witnessRepository.save(targetWitness);
                updatedWitnessList.add(targetWitness);
            } else {
                Person wPerson = null;
                if (wDto.personId() != null) {
                    wPerson = personRepository.findById(wDto.personId()).orElse(null);
                }
                if (wPerson == null && wDto.firstName() != null && !wDto.firstName().isBlank()) {
                    wPerson = new Person();
                    wPerson.setFirstName(wDto.firstName().trim());
                    wPerson.setLastName(wDto.lastName() != null ? wDto.lastName().trim() : "");
                    wPerson.setContactNumber(wDto.contactNumber());
                    wPerson.setCompleteAddress(wDto.completeAddress());
                    wPerson.setIsResident(false);
                    wPerson = personRepository.save(wPerson);
                }
                if (wPerson != null) {
                    Witness newWitness = witnessMapper.toWitness(wPerson, aCase, wDto);
                    newWitness = witnessRepository.save(newWitness);
                    updatedWitnessList.add(newWitness);
                }
            }
            inputIdx++;
        }

        if (aCase.getWitnesses() != null) {
            aCase.getWitnesses().clear();
            aCase.getWitnesses().addAll(updatedWitnessList);
        } else {
            aCase.setWitnesses(new ArrayList<>(updatedWitnessList));
        }
    }
}
