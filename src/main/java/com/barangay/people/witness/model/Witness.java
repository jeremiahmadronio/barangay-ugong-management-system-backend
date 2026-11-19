package com.barangay.people.witness.model;


import com.barangay.cases.shared.model.Case;
import com.barangay.people.person.model.Person;
import com.barangay.zshared.entity.BaseEntity;
import com.barangay.zshared.security.EncryptedFieldConverter;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "witnesses", indexes = {
        @Index(name = "idx_witness_case_id", columnList = "case_id"),
        @Index(name = "idx_witness_person_id", columnList = "person_id")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Witness extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case aCase;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "person_id")
    private Person person;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(columnDefinition = "TEXT")
    private String testimony;

    @Column(name = "testimony_file_name")
    private String testimonyFileName;

    @JdbcTypeCode(Types.VARBINARY)
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "testimony_file", columnDefinition = "bytea")
    private byte[] testimonyFile;
}

