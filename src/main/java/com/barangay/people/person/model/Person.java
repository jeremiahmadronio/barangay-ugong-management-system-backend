package com.barangay.people.person.model;

import com.barangay.people.complainant.model.Complainant;
import com.barangay.people.resident.model.Resident;
import com.barangay.people.respondent.model.Respondent;
import com.barangay.people.witness.model.Witness;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.entity.BaseEntity;
import com.barangay.zshared.security.EncryptedFieldConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @OneToMany(mappedBy = "person", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<User> users = new ArrayList<>();

    @Transient
    @JsonIgnore
    public User getUser() {
        if (users == null || users.isEmpty()) return null;
        return users.stream()
                .filter(u -> !Boolean.TRUE.equals(u.getIsPermanentDeleted()))
                .findFirst()
                .orElse(users.get(0));
    }

    @Transient
    public int getCalculatedAge() {
        if (birthDate == null) return age != null ? age : 0;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    @Transient
    public String getDisplayEmail() {
        if (email != null && !email.isBlank()) {
            return email.trim();
        }
        User activeUser = getUser();
        if (activeUser != null && activeUser.getSystemEmail() != null && !activeUser.getSystemEmail().isBlank()) {
            return activeUser.getSystemEmail().trim();
        }
        return null;
    }
}
