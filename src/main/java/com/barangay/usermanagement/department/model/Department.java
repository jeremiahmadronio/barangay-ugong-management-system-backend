package com.barangay.usermanagement.department.model;


import com.barangay.zshared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Department extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, unique = true, nullable = false)
    private String name;

    @Column
    private String description;

    @Column(name = "is_active")
    private Boolean isActive = true;
}