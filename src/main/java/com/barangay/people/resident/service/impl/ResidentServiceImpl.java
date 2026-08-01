package com.barangay.people.resident.service.impl;

import com.barangay.people.person.model.Person;
import com.barangay.people.person.repository.PersonRepository;
import com.barangay.people.resident.dto.*;
import com.barangay.people.resident.model.Resident;
import com.barangay.people.resident.repository.ResidentRepository;
import com.barangay.people.resident.service.ResidentService;
import com.barangay.usermanagement.user.dto.StatMetric;
import com.barangay.usermanagement.user.dto.UserDetailResponse.PersonSummaryInfo;
import com.barangay.zshared.enumerated.ResidentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResidentServiceImpl implements ResidentService {

    private final ResidentRepository residentRepository;
    private final PersonRepository personRepository;
    private final com.barangay.people.person.repository.RelationshipRepository relationshipRepository;
    private final com.barangay.people.resident.repository.ResidentDocumentRepository residentDocumentRepository;

    @Override
    @Transactional(readOnly = true)
    public ResidentManagementStatsResponse getResidentStats() {
        List<ResidentStatus> activeAndInactive = List.of(ResidentStatus.ACTIVE, ResidentStatus.INACTIVE);

        long totalCount = residentRepository.countByStatusIn(activeAndInactive);
        long headsOfFamilyCount = residentRepository.countByStatusInAndIsHeadOfFamilyTrue(activeAndInactive);
        
        LocalDate seniorCutoff = LocalDate.now().minusYears(60);
        long seniorCount = residentRepository.countSeniorCitizensByStatuses(seniorCutoff, activeAndInactive);

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfThisMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfThisMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        YearMonth previousMonth = currentMonth.minusMonths(1);
        LocalDateTime startOfLastMonth = previousMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfLastMonth = previousMonth.atEndOfMonth().atTime(23, 59, 59);

        long thisMonthCount = residentRepository.countByStatusInAndCreatedAtBetween(activeAndInactive, startOfThisMonth, endOfThisMonth);
        long lastMonthCreatedCount = residentRepository.countByStatusInAndCreatedAtBetween(activeAndInactive, startOfLastMonth, endOfLastMonth);

        LocalDateTime epochStart = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
        long lastMonthTotalCount = residentRepository.countByStatusInAndCreatedAtBetween(activeAndInactive, epochStart, endOfLastMonth);
        long lastMonthHeadsCount = residentRepository.countByStatusInAndIsHeadOfFamilyTrueAndCreatedAtBetween(activeAndInactive, epochStart, endOfLastMonth);
        long lastMonthSeniorCount = residentRepository.countSeniorCitizensByStatusesBetween(seniorCutoff, activeAndInactive, epochStart, endOfLastMonth);

        StatMetric totalMetric = calculateMetric(totalCount, lastMonthTotalCount);
        StatMetric headsMetric = calculateMetric(headsOfFamilyCount, lastMonthHeadsCount);
        StatMetric seniorMetric = calculateMetric(seniorCount, lastMonthSeniorCount);
        StatMetric thisMonthMetric = calculateMetric(thisMonthCount, lastMonthCreatedCount);

        return new ResidentManagementStatsResponse(totalMetric, headsMetric, seniorMetric, thisMonthMetric);
    }

    private StatMetric calculateMetric(long current, long previous) {
        if (current == 0) {
            return new StatMetric(0L, 0.0, "NEUTRAL", "0.0% vs last month");
        }
        if (previous == 0) {
            return new StatMetric(current, 100.0, "UP", "+100.0% vs last month");
        }
        double diff = ((double) (current - previous) / (double) current) * 100.0;
        double rounded = Math.round(diff * 10.0) / 10.0;
        String direction = rounded > 0 ? "UP" : rounded < 0 ? "DOWN" : "NEUTRAL";
        String text = String.format("%s%.1f%% vs last month", rounded > 0 ? "+" : "", rounded);
        return new StatMetric(current, rounded, direction, text);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResidentTableResponse> getFilteredResidentsTable(ResidentFilterRequest filter) {
        String searchPattern = (filter != null && filter.search() != null && !filter.search().isBlank())
                ? "%" + filter.search().trim().toLowerCase() + "%"
                : null;

        ResidentStatus targetStatus = null;
        if (filter != null && filter.status() != null && !filter.status().isBlank() && !filter.status().equalsIgnoreCase("all")) {
            try {
                targetStatus = ResidentStatus.valueOf(filter.status().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                targetStatus = null;
            }
        }

        String gender = (filter != null && filter.gender() != null && !filter.gender().isBlank()) ? filter.gender().trim() : null;
        String ageGroup = filter != null ? filter.ageGroup() : null;

        List<Resident> residents = residentRepository.findFilteredResidents(searchPattern, targetStatus, gender);

        return residents.stream()
                .filter(r -> matchesAgeGroup(r, ageGroup))
                .map(this::mapToTableResponse)
                .collect(Collectors.toList());
    }

    private boolean matchesAgeGroup(Resident resident, String ageGroup) {
        if (ageGroup == null || ageGroup.isBlank() || ageGroup.equalsIgnoreCase("all")) {
            return true;
        }
        int age = resident.getPerson() != null ? resident.getPerson().getCalculatedAge() : 0;
        switch (ageGroup.toUpperCase()) {
            case "YOUTH":
                return age >= 0 && age <= 17;
            case "ADULT":
                return age >= 18 && age <= 59;
            case "SENIOR":
                return age >= 60;
            default:
                return true;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResidentDetailResponse getResidentDetail(Long id) {
        Resident resident = residentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resident not found with id: " + id));
        return mapToDetailResponse(resident);
    }

    @Override
    @Transactional
    public ResidentTableResponse createResident(ResidentCreateRequest request) {
        Person person = new Person();
        mapRequestToPerson(request, person);
        person.setIsResident(true);
        person = personRepository.save(person);

        Resident resident = new Resident();
        resident.setPerson(person);
        resident.setBarangayIdNumber(request.barangayIdNumber());
        resident.setHouseholdNumber(request.householdNumber());
        resident.setIsHeadOfFamily(Boolean.TRUE.equals(request.isHeadOfFamily()));
        resident.setEducationalAttainment(request.educationalAttainment());
        resident.setCitizenship(request.citizenship() != null ? request.citizenship() : "Filipino");
        resident.setDateOfResidency(request.dateOfResidency() != null ? request.dateOfResidency() : LocalDate.now());
        resident.setStatus(request.status() != null ? request.status() : ResidentStatus.ACTIVE);

        resident = residentRepository.save(resident);

        // Save family associations if provided
        if (request.familyAssociations() != null && !request.familyAssociations().isEmpty()) {
            saveFamilyAssociations(person, request.familyAssociations());
        }

        // Save attached files if provided
        if (request.attachedFiles() != null && !request.attachedFiles().isEmpty()) {
            saveAttachedFiles(resident, request.attachedFiles());
        }

        return mapToTableResponse(resident);
    }

    @Override
    @Transactional
    public ResidentTableResponse updateResident(Long id, ResidentUpdateRequest request) {
        Resident resident = residentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resident not found with id: " + id));

        Person person = resident.getPerson();
        if (person == null) {
            person = new Person();
            person.setIsResident(true);
        }
        mapUpdateRequestToPerson(request, person);
        personRepository.save(person);

        if (request.barangayIdNumber() != null) resident.setBarangayIdNumber(request.barangayIdNumber());
        if (request.householdNumber() != null) resident.setHouseholdNumber(request.householdNumber());
        if (request.isHeadOfFamily() != null) resident.setIsHeadOfFamily(request.isHeadOfFamily());
        if (request.educationalAttainment() != null) resident.setEducationalAttainment(request.educationalAttainment());
        if (request.citizenship() != null) resident.setCitizenship(request.citizenship());
        if (request.dateOfResidency() != null) resident.setDateOfResidency(request.dateOfResidency());
        if (request.status() != null) resident.setStatus(request.status());
        if (request.statusRemarks() != null) resident.setStatusRemarks(request.statusRemarks());

        resident = residentRepository.save(resident);

        // Save family associations if provided
        if (request.familyAssociations() != null && !request.familyAssociations().isEmpty()) {
            saveFamilyAssociations(person, request.familyAssociations());
        }

        // Save attached files if provided
        if (request.attachedFiles() != null && !request.attachedFiles().isEmpty()) {
            saveAttachedFiles(resident, request.attachedFiles());
        }

        return mapToTableResponse(resident);
    }

    private void saveFamilyAssociations(Person person, List<FamilyAssociationRequest> associations) {
        for (FamilyAssociationRequest fa : associations) {
            if (fa.relatedResidentId() == null) continue;
            residentRepository.findById(fa.relatedResidentId()).ifPresent(otherRes -> {
                Person otherPerson = otherRes.getPerson();
                if (otherPerson != null && !otherPerson.getId().equals(person.getId())) {
                    if (!relationshipRepository.existsByPersonIdAndRelativeId(person.getId(), otherPerson.getId())) {
                        com.barangay.people.person.model.Relationship r1 = new com.barangay.people.person.model.Relationship();
                        r1.setPerson(person);
                        r1.setRelative(otherPerson);
                        r1.setRelationshipType(fa.relationship() != null ? fa.relationship() : "Relative");
                        relationshipRepository.save(r1);
                    }

                    if (!relationshipRepository.existsByPersonIdAndRelativeId(otherPerson.getId(), person.getId())) {
                        com.barangay.people.person.model.Relationship r2 = new com.barangay.people.person.model.Relationship();
                        r2.setPerson(otherPerson);
                        r2.setRelative(person);
                        r2.setRelationshipType(fa.reverseRelationship() != null ? fa.reverseRelationship() : "Relative");
                        relationshipRepository.save(r2);
                    }
                }
            });
        }
    }

    private void saveAttachedFiles(Resident resident, List<ResidentFileRequest> files) {
        for (ResidentFileRequest f : files) {
            if (f.fileBase64() == null || f.fileBase64().isBlank()) continue;
            byte[] fileBytes = parsePhotoBase64(f.fileBase64());
            if (fileBytes == null) continue;

            com.barangay.people.resident.model.ResidentDocument doc = new com.barangay.people.resident.model.ResidentDocument();
            doc.setResident(resident);
            doc.setDocumentName(f.fileName() != null ? f.fileName() : "document");
            doc.setDocumentType(f.fileType() != null ? f.fileType() : "application/octet-stream");
            doc.setFileData(fileBytes);
            doc.setStatus(com.barangay.zshared.enumerated.ResidentDocumentStatus.VERIFIED);
            residentDocumentRepository.save(doc);
        }
    }

    @Override
    @Transactional
    public ResidentTableResponse updateResidentStatus(Long id, ResidentStatusUpdateRequest request) {
        Resident resident = residentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resident not found with id: " + id));

        resident.setStatus(request.status());
        if (request.remarks() != null) {
            resident.setStatusRemarks(request.remarks());
        }
        resident = residentRepository.save(resident);
        return mapToTableResponse(resident);
    }

    @Override
    @Transactional
    public ResidentTableResponse archiveResident(Long id) {
        Resident resident = residentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resident not found with id: " + id));
        resident.setStatus(ResidentStatus.ARCHIVED);
        resident.setStatusRemarks("Archived from table action");
        resident = residentRepository.save(resident);
        return mapToTableResponse(resident);
    }

    @Override
    @Transactional
    public ResidentTableResponse restoreResident(Long id) {
        Resident resident = residentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resident not found with id: " + id));
        resident.setStatus(ResidentStatus.ACTIVE);
        resident.setStatusRemarks("Restored to active status");
        resident = residentRepository.save(resident);
        return mapToTableResponse(resident);
    }

    @Override
    @Transactional
    public void deleteResident(Long id) {
        Resident resident = residentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resident not found with id: " + id));
        resident.setStatus(ResidentStatus.INACTIVE);
        residentRepository.save(resident);
    }

    @Override
    @Transactional(readOnly = true)
    public ResidentSuggestionsResponse getSuggestions() {
        // Derive next Barangay ID from the latest resident record
        String nextBarangayId = residentRepository.findLatestResident()
                .map(r -> {
                    long nextId = r.getResidentId() + 1;
                    int year = java.time.Year.now().getValue();
                    return String.format("BRGY-%d-%04d", year, nextId);
                })
                .orElse(String.format("BRGY-%d-0001", java.time.Year.now().getValue()));

        // Suggest the same household number as the latest resident
        String suggestedHousehold = residentRepository.findLatestHouseholdNumber()
                .orElse("HH-001");

        return new ResidentSuggestionsResponse(nextBarangayId, suggestedHousehold);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResidentSearchOptionResponse> searchActiveResidents(String search) {
        String query = search != null ? search.trim() : "";
        return residentRepository.searchActiveResidents(query)
                .stream()
                .map(r -> {
                    var p = r.getPerson();
                    String fullName = p != null ? buildFullName(p.getFirstName(), p.getMiddleName(), p.getLastName(), p.getSuffix()) : "—";
                    String photoStr = p != null && p.getPhoto() != null ? Base64.getEncoder().encodeToString(p.getPhoto()) : null;
                    return new ResidentSearchOptionResponse(
                            r.getResidentId(),
                            p != null ? p.getId() : null,
                            fullName,
                            p != null ? p.getFirstName() : null,
                            p != null ? p.getLastName() : null,
                            p != null ? p.getMiddleName() : null,
                            r.getBarangayIdNumber(),
                            p != null ? p.getEmail() : null,
                            p != null ? p.getContactNumber() : null,
                            p != null ? p.getCompleteAddress() : null,
                            p != null ? p.getCalculatedAge() : null,
                            p != null ? p.getGender() : null,
                            p != null ? p.getCivilStatus() : null,
                            r.getStatus() != null ? r.getStatus().name() : "ACTIVE",
                            photoStr
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResidentRelativeResponse> getRelativesByLastName(String lastName) {
        if (lastName == null || lastName.isBlank()) return java.util.Collections.emptyList();
        return residentRepository.findActiveResidentsByLastName(lastName.trim())
                .stream()
                .map(r -> {
                    var p = r.getPerson();
                    String fullName = p != null ? buildFullName(p.getFirstName(), p.getMiddleName(), p.getLastName(), p.getSuffix()) : "—";
                    int age = p != null ? p.getCalculatedAge() : 0;
                    String photoStr = p != null && p.getPhoto() != null ? Base64.getEncoder().encodeToString(p.getPhoto()) : null;
                    return new ResidentRelativeResponse(
                            r.getResidentId(),
                            fullName,
                            p != null ? p.getCompleteAddress() : null,
                            age,
                            p != null ? p.getGender() : null,
                            p != null ? p.getCivilStatus() : null,
                            p != null ? p.getContactNumber() : null,
                            r.getHouseholdNumber(),
                            r.getBarangayIdNumber(),
                            r.getStatus() != null ? r.getStatus().name() : "ACTIVE",
                            "Relative",
                            photoStr
                    );
                })
                .collect(Collectors.toList());
    }

    private void mapRequestToPerson(ResidentCreateRequest req, Person p) {
        p.setFirstName(req.firstName());
        p.setLastName(req.lastName());
        p.setMiddleName(req.middleName());
        p.setSuffix(req.suffix());
        p.setContactNumber(req.contactNumber());
        p.setEmail(req.email());
        p.setCompleteAddress(req.completeAddress());
        p.setGender(req.gender());
        p.setCivilStatus(req.civilStatus());
        p.setOccupation(req.occupation());
        p.setBirthDate(req.birthDate());
        if (req.age() != null) p.setAge(req.age());
        if (req.photoBase64() != null && !req.photoBase64().isBlank()) {
            System.out.println("Processing Create photoBase64, raw length: " + req.photoBase64().length());
            byte[] photoBytes = parsePhotoBase64(req.photoBase64());
            if (photoBytes != null && photoBytes.length > 0) {
                p.setPhoto(photoBytes);
                System.out.println("Successfully saved photo byte array of size: " + photoBytes.length + " bytes");
            } else {
                System.err.println("Failed to decode photoBase64 string");
            }
        }
    }

    private void mapUpdateRequestToPerson(ResidentUpdateRequest req, Person p) {
        p.setFirstName(req.firstName());
        p.setLastName(req.lastName());
        p.setMiddleName(req.middleName());
        p.setSuffix(req.suffix());
        p.setContactNumber(req.contactNumber());
        p.setEmail(req.email());
        p.setCompleteAddress(req.completeAddress());
        p.setGender(req.gender());
        p.setCivilStatus(req.civilStatus());
        p.setOccupation(req.occupation());
        p.setBirthDate(req.birthDate());
        if (req.age() != null) p.setAge(req.age());
        if (req.photoBase64() != null && !req.photoBase64().isBlank()) {
            System.out.println("Processing Update photoBase64, raw length: " + req.photoBase64().length());
            byte[] photoBytes = parsePhotoBase64(req.photoBase64());
            if (photoBytes != null && photoBytes.length > 0) {
                p.setPhoto(photoBytes);
                System.out.println("Successfully updated photo byte array of size: " + photoBytes.length + " bytes");
            } else {
                System.err.println("Failed to decode photoBase64 string");
            }
        }
    }

    private byte[] parsePhotoBase64(String photoBase64) {
        if (photoBase64 == null || photoBase64.isBlank()) return null;
        String clean = photoBase64.trim();
        if (clean.contains(",")) {
            clean = clean.substring(clean.indexOf(",") + 1);
        }
        clean = clean.replaceAll("\\s+", "");
        try {
            return Base64.getMimeDecoder().decode(clean);
        } catch (Exception e) {
            try {
                return Base64.getDecoder().decode(clean);
            } catch (Exception ex) {
                System.err.println("Base64 decode error: " + ex.getMessage());
                return null;
            }
        }
    }

    private ResidentTableResponse mapToTableResponse(Resident r) {
        Person p = r.getPerson();
        String photoStr = p != null && p.getPhoto() != null ? Base64.getEncoder().encodeToString(p.getPhoto()) : null;
        String fullName = p != null ? buildFullName(p.getFirstName(), p.getMiddleName(), p.getLastName(), p.getSuffix()) : "—";
        int age = p != null ? p.getCalculatedAge() : 0;

        return new ResidentTableResponse(
                r.getResidentId(),
                r.getBarangayIdNumber() != null ? r.getBarangayIdNumber() : "BRGY-" + String.format("%04d", r.getResidentId()),
                photoStr,
                fullName,
                p != null ? p.getFirstName() : "",
                p != null ? p.getLastName() : "",
                p != null ? p.getMiddleName() : "",
                p != null ? p.getContactNumber() : null,
                p != null ? p.getEmail() : null,
                p != null ? p.getCompleteAddress() : null,
                r.getHouseholdNumber() != null ? r.getHouseholdNumber() : "—",
                Boolean.TRUE.equals(r.getIsHeadOfFamily()),
                r.getStatus() != null ? r.getStatus() : ResidentStatus.ACTIVE,
                age,
                p != null ? p.getGender() : "—",
                p != null ? p.getCivilStatus() : "—",
                r.getDateOfResidency(),
                r.getCreatedAt()
        );
    }

    private ResidentDetailResponse mapToDetailResponse(Resident r) {
        Person p = r.getPerson();
        String photoStr = p != null && p.getPhoto() != null ? Base64.getEncoder().encodeToString(p.getPhoto()) : null;
        String fullName = p != null ? buildFullName(p.getFirstName(), p.getMiddleName(), p.getLastName(), p.getSuffix()) : "—";
        int age = p != null ? p.getCalculatedAge() : 0;

        PersonSummaryInfo personInfo = new PersonSummaryInfo(
                p != null ? p.getId() : null,
                fullName,
                p != null ? p.getFirstName() : null,
                p != null ? p.getMiddleName() : null,
                p != null ? p.getLastName() : null,
                p != null ? p.getSuffix() : null,
                p != null ? p.getGender() : null,
                p != null ? p.getBirthDate() : null,
                age,
                p != null ? p.getCivilStatus() : null,
                p != null ? p.getContactNumber() : null,
                p != null ? p.getCompleteAddress() : null,
                p != null ? p.getOccupation() : null
        );

        List<ResidentRelativeResponse> familyAssociations = java.util.Collections.emptyList();
        if (p != null) {
            List<com.barangay.people.person.model.Relationship> rels = relationshipRepository.findByPersonId(p.getId());
            if (rels != null && !rels.isEmpty()) {
                java.util.Map<Long, ResidentRelativeResponse> relativeMap = new java.util.LinkedHashMap<>();
                for (com.barangay.people.person.model.Relationship rel : rels) {
                    Person otherP = rel.getRelative();
                    if (otherP != null && !otherP.getId().equals(p.getId()) && !relativeMap.containsKey(otherP.getId())) {
                        Resident otherR = otherP.getResident();
                        String otherFullName = buildFullName(otherP.getFirstName(), otherP.getMiddleName(), otherP.getLastName(), otherP.getSuffix());
                        int otherAge = otherP.getCalculatedAge();
                        String otherPhotoStr = otherP.getPhoto() != null ? Base64.getEncoder().encodeToString(otherP.getPhoto()) : null;

                        relativeMap.put(otherP.getId(), new ResidentRelativeResponse(
                                otherR != null ? otherR.getResidentId() : otherP.getId(),
                                otherFullName,
                                otherP.getCompleteAddress(),
                                otherAge,
                                otherP.getGender(),
                                otherP.getCivilStatus(),
                                otherP.getContactNumber(),
                                otherR != null ? otherR.getHouseholdNumber() : "—",
                                otherR != null ? otherR.getBarangayIdNumber() : "—",
                                otherR != null && otherR.getStatus() != null ? otherR.getStatus().name() : "ACTIVE",
                                rel.getRelationshipType(),
                                otherPhotoStr
                        ));
                    }
                }
                familyAssociations = new ArrayList<>(relativeMap.values());
            }
        }

        List<ResidentFileResponse> attachedFiles = java.util.Collections.emptyList();
        List<com.barangay.people.resident.model.ResidentDocument> docs = residentDocumentRepository.findByResidentResidentId(r.getResidentId());
        if (docs != null && !docs.isEmpty()) {
            attachedFiles = docs.stream().map(doc -> {
                String fileStr = doc.getFileData() != null ? Base64.getEncoder().encodeToString(doc.getFileData()) : null;
                return new ResidentFileResponse(
                        doc.getId(),
                        doc.getDocumentName(),
                        doc.getDocumentType(),
                        fileStr
                );
            }).collect(Collectors.toList());
        }

        return new ResidentDetailResponse(
                r.getResidentId(),
                r.getBarangayIdNumber() != null ? r.getBarangayIdNumber() : "BRGY-" + String.format("%04d", r.getResidentId()),
                r.getHouseholdNumber(),
                Boolean.TRUE.equals(r.getIsHeadOfFamily()),
                r.getStatus(),
                r.getStatusRemarks(),
                r.getCitizenship(),
                r.getEducationalAttainment(),
                r.getDateOfResidency(),
                personInfo,
                photoStr,
                familyAssociations,
                attachedFiles,
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }

    private String buildFullName(String first, String middle, String last, String suffix) {
        StringBuilder sb = new StringBuilder();
        if (first != null && !first.isBlank()) sb.append(first.trim());
        if (middle != null && !middle.isBlank()) sb.append(" ").append(middle.trim());
        if (last != null && !last.isBlank()) sb.append(" ").append(last.trim());
        if (suffix != null && !suffix.isBlank()) sb.append(" ").append(suffix.trim());
        return sb.toString().trim();
    }
}

