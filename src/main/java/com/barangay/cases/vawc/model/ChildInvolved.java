package com.barangay.cases.vawc.model;

import com.barangay.cases.shared.model.Case;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "children_involved", indexes = {
        @Index(name = "idx_child_involved_case_id", columnList = "case_id")
})
public class ChildInvolved {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "child_name")
    private String name;

    private Integer age;

    @Column(name = "child_role")
    private String childRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    private Case cases;

}