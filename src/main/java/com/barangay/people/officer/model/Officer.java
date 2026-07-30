package com.barangay.people.officer.model;

import com.barangay.people.person.model.Person;
import com.barangay.user_management.department.model.Department;
import com.barangay.zshared.entity.BaseEntity;
import com.barangay.zshared.enumerated.OfficerStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "officer")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Officer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String position;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private OfficerStatus status = OfficerStatus.ACTIVE;

    @Column(name = "is_global")
    private Boolean isGlobal = false;

    @Column(name = "active_cases_count")
    private Integer activeCasesCount = 0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "person_id", referencedColumnName = "id")
    private Person person;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id", referencedColumnName = "id")
    private Department department;
}
