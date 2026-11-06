package com.barangay.cases.shared.model;

import com.barangay.zshared.entity.BaseEntity;
import com.barangay.zshared.security.EncryptedFieldConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;

@Entity
@Table(name = "narrative_statement")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NarrativeStatement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "statement_summary", columnDefinition = "TEXT")
    private String statementSummary;

    @JdbcTypeCode(Types.VARBINARY)
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "statement", columnDefinition = "bytea")
    private byte[] fileData;

}
