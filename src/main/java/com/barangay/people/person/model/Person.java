package com.barangay.people.person.model;

import com.barangay.people.complainant.model.Complainant;
import com.barangay.people.resident.model.Resident;
import com.barangay.people.respondent.model.Respondent;
import com.barangay.people.witness.model.Witness;
import com.barangay.zshared.entity.BaseEntity;
import com.barangay.zshared.security.EncryptedFieldConverter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "person")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Person extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(Types.VARBINARY)
    @Column(name = "photo", columnDefinition = "bytea")
    private byte[] photo;

    @Column(length = 100, nullable = false, name = "last_name")
    @JdbcTypeCode(Types.VARCHAR)
    private String lastName;

    @Column(length = 100, nullable = false, name = "first_name")
    @JdbcTypeCode(Types.VARCHAR)
    private String firstName;

    @Column(length = 100, name = "middle_name")
    private String middleName;

    @Column(length = 10)
    private String suffix;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "contact_number", columnDefinition = "TEXT")
    private String contactNumber;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(columnDefinition = "TEXT", name = "complete_address")
    private String completeAddress;

    @Column(name = "is_resident")
    private Boolean isResident = false;

    @Column(name = "age")
    private Short age;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 20, name = "gender")
    private String gender;

    @Column(length = 50, name = "civil_status")
    private String civilStatus;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "email", columnDefinition = "TEXT")
    private String email;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "occupation", columnDefinition = "TEXT")
    private String occupation;

    @Column(length = 255, name = "grade_school")
    private String gradeSchool;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "guardian_name", columnDefinition = "TEXT")
    private String guardianName;

    @OneToOne(mappedBy = "person", cascade = CascadeType.ALL)
    @JsonManagedReference
    private Resident resident;

    @OneToMany(mappedBy = "person", fetch = FetchType.LAZY)
    private List<Complainant> asComplainant = new ArrayList<>();

    @OneToMany(mappedBy = "person", fetch = FetchType.LAZY)
    private List<Respondent> asRespondent = new ArrayList<>();

    @OneToMany(mappedBy = "person", fetch = FetchType.LAZY)
    private List<Witness> asWitness = new ArrayList<>();

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Relationship> relationships = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public byte[] getPhoto() { return photo; }
    public void setPhoto(byte[] photo) { this.photo = photo; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getSuffix() { return suffix; }
    public void setSuffix(String suffix) { this.suffix = suffix; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getCompleteAddress() { return completeAddress; }
    public void setCompleteAddress(String completeAddress) { this.completeAddress = completeAddress; }

    public Boolean getIsResident() { return isResident; }
    public void setIsResident(Boolean isResident) { this.isResident = isResident; }

    public Short getAge() { return age; }
    public void setAge(Short age) { this.age = age; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getCivilStatus() { return civilStatus; }
    public void setCivilStatus(String civilStatus) { this.civilStatus = civilStatus; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }

    public String getGradeSchool() { return gradeSchool; }
    public void setGradeSchool(String gradeSchool) { this.gradeSchool = gradeSchool; }

    public String getGuardianName() { return guardianName; }
    public void setGuardianName(String guardianName) { this.guardianName = guardianName; }

    public Resident getResident() { return resident; }
    public void setResident(Resident resident) { this.resident = resident; }

    public List<Complainant> getAsComplainant() { return asComplainant; }
    public void setAsComplainant(List<Complainant> asComplainant) { this.asComplainant = asComplainant; }

    public List<Respondent> getAsRespondent() { return asRespondent; }
    public void setAsRespondent(List<Respondent> asRespondent) { this.asRespondent = asRespondent; }

    public List<Witness> getAsWitness() { return asWitness; }
    public void setAsWitness(List<Witness> asWitness) { this.asWitness = asWitness; }

    public List<Relationship> getRelationships() { return relationships; }
    public void setRelationships(List<Relationship> relationships) { this.relationships = relationships; }

    @Transient
    public int getCalculatedAge() {
        if (birthDate == null) return age != null ? age : 0;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
