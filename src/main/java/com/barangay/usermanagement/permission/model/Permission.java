package com.barangay.usermanagement.permission.model;

import com.barangay.zshared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "permission")
@Getter @Setter
 @NoArgsConstructor
public class Permission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(unique = true, nullable = false, length = 100)
    private String permissionName;

    @Column(length = 50)
    private String module;

    @Column(columnDefinition = "TEXT")
    private String description;

}
