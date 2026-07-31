package com.barangay.people.witness.model;

import com.barangay.cases.sharedmodel.Case;
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
@Table(name = "witnesses")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(columnDefinition = "TEXT")
    private String testimony;

    @JdbcTypeCode(Types.VARBINARY)
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "testimony_file", columnDefinition = "bytea")
    private byte[] testimonyFile;
}
