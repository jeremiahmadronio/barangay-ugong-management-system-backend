package com.barangay.zshared.mapper;

import com.barangay.people.complainant.model.Complainant;
import com.barangay.people.person.model.Person;
import com.barangay.people.resident.model.Resident;
import com.barangay.zshared.dto.ComplainantCreateDto;
import com.barangay.zshared.dto.ComplainantDetailDto;
import com.barangay.zshared.dto.ResidentDetailDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ResidentMapper {

    private final PersonMapper personMapper;

    public ResidentDetailDto toDetailDto(Complainant complainant) {
        if (complainant == null || complainant.getPerson() == null) return null;
        Person p = complainant.getPerson();

        return new ResidentDetailDto(
                complainant.getId(),
                p.getLastName(),
                p.getFirstName(),
                p.getMiddleName(),
                personMapper.buildFullName(p),
                p.getContactNumber(),
                p.getAge(),
                p.getGender(),
                p.getCivilStatus(),
                p.getEmail(),
                personMapper.getCompleteAddress(p),
                p.getResident().getDateOfResidency(),
                p.getResident().getBarangayIdNumber()

        );
    }


    public ResidentDetailDto toDetailDto(Resident resident) {
        if (resident == null || resident.getPerson() == null) return null;
        Person p = resident.getPerson();

        return new ResidentDetailDto(
                resident.getResidentId(),
                p.getLastName(),
                p.getFirstName(),
                p.getMiddleName(),
                personMapper.buildFullName(p),
                p.getContactNumber(),
                p.getAge(),
                p.getGender(),
                p.getCivilStatus(),
                p.getEmail(),
                personMapper.getCompleteAddress(p),
                resident.getDateOfResidency(),
                resident.getBarangayIdNumber()
        );
    }


}
