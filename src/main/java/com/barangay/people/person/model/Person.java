package com.barangay.people.person.model;

import com.barangay.zshared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDate;
import java.time.Period;


@Entity
@Table(name = "person")
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class Person extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
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

    @Column(name = "contact_number", length = 50)
    private String contactNumber;

    @Column(name = "complete_address", columnDefinition = "TEXT")
    private String completeAddress;

    @Column(name = "is_resident")
    private Boolean isResident = false;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Transient
    public int getAge() {
        if (birthDate == null) return 0;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    @Column(length = 20, name = "gender")
    private String gender;

    @Column(name = "civil_status")
    private String civilStatus;

    @Column(length = 255, name = "email")
    private String email;

    @Column(name = "occupation", columnDefinition = "TEXT")
    private String occupation;
}
