package com.barangay.people.resident.service;

import com.barangay.people.person.model.Person;
import com.barangay.people.resident.model.Resident;
import com.barangay.people.resident.repository.ResidentRepository;
import com.barangay.zshared.enumerated.ResidentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveResidentService {


    private final ResidentRepository residentRepository;


    @Transactional(readOnly = true)
    public List<Map<String, Object>> getArchivedResidents(String search) {
        try {
            List<Resident> residents = residentRepository.findAllByStatus(ResidentStatus.ARCHIVED);
            String query = search != null ? search.toLowerCase().trim() : "";

            return residents.stream()
                    .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()) && !Boolean.TRUE.equals(r.getIsPermanentDeleted()))
                    .filter(r -> {
                        if (query.isEmpty()) return true;
                        Person p = r.getPerson();
                        String fullName = p != null ? (p.getFirstName() + " " + p.getLastName()).toLowerCase() : "";
                        String barangayId = r.getBarangayIdNumber() != null ? r.getBarangayIdNumber().toLowerCase() : "";
                        return fullName.contains(query) || barangayId.contains(query);
                    })
                    .map(r -> {
                        Person p = r.getPerson();
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", r.getResidentId());
                        map.put("fullName", p != null ? (p.getFirstName() + " " + (p.getMiddleName() != null ? p.getMiddleName() + " " : "") + p.getLastName()) : "N/A");
                        map.put("barangayId", r.getBarangayIdNumber() != null ? r.getBarangayIdNumber() : "N/A");
                        map.put("contact", p != null && p.getContactNumber() != null ? p.getContactNumber() : "N/A");
                        map.put("status", "ARCHIVED");
                        return map;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("getArchivedResidents fallback: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Transactional
    public void restoreResident(Long residentId) {
        Resident resident = residentRepository.findById(residentId).orElse(null);
        if (resident != null) {
            resident.setStatus(ResidentStatus.ACTIVE);
            resident.restoreDelete();
            residentRepository.save(resident);
        }
    }

    @Transactional
    public void softDeleteResident(Long residentId) {
        Resident resident = residentRepository.findById(residentId).orElse(null);
        if (resident != null) {
            resident.softDelete();
            resident.setStatus(ResidentStatus.INACTIVE);
            residentRepository.save(resident);
        }
    }

    @Transactional
    public void deleteResidentPermanent(Long residentId) {
        Resident resident = residentRepository.findById(residentId).orElse(null);
        if (resident != null) {
            resident.permanentDelete();
            residentRepository.save(resident);
        }
    }


    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTrashResidents(String search) {
        try {
            List<Resident> residents = residentRepository.findAll();
            String query = search != null ? search.toLowerCase().trim() : "";

            return residents.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getIsDeleted()) && !Boolean.TRUE.equals(r.getIsPermanentDeleted()))
                    .filter(r -> {
                        if (query.isEmpty()) return true;
                        Person p = r.getPerson();
                        String fullName = p != null ? (p.getFirstName() + " " + p.getLastName()).toLowerCase() : "";
                        String barangayId = r.getBarangayIdNumber() != null ? r.getBarangayIdNumber().toLowerCase() : "";
                        return fullName.contains(query) || barangayId.contains(query);
                    })
                    .map(r -> {
                        Person p = r.getPerson();
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", r.getResidentId());
                        map.put("fullName", p != null ? (p.getFirstName() + " " + (p.getMiddleName() != null ? p.getMiddleName() + " " : "") + p.getLastName()) : "N/A");
                        map.put("barangayId", r.getBarangayIdNumber() != null ? r.getBarangayIdNumber() : "N/A");
                        map.put("contact", p != null && p.getContactNumber() != null ? p.getContactNumber() : "N/A");
                        map.put("status", "DELETED");
                        map.put("deletedAt", r.getDeletedAt() != null ? r.getDeletedAt().toString() : "");
                        return map;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("getTrashResidents fallback: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

}
