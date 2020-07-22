package ru.citeck.ecos.uiserv.domain.journal.repo;

import lombok.Data;
import ru.citeck.ecos.uiserv.domain.common.repo.AbstractAuditingEntity;

import javax.persistence.*;

@Entity
@Table(name = "journal")
@Data
public class JournalEntity extends AbstractAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "journal_seq_gen")
    @SequenceGenerator(name = "journal_seq_gen")
    private Long id;

    @Column(unique = true)
    private String extId;

    private String label;

    private String sourceId;

    private String groupBy;

    private String sortBy;

    private String metaRecord;

    private String typeRef;

    private String predicate;

    private Boolean editable;

    private String attributes;

    private String columns;

    private String actions;

    private String groupActions;
}
