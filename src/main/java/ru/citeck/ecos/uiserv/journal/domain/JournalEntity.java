package ru.citeck.ecos.uiserv.journal.domain;

import lombok.Data;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.domain.ActionEntity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "journal")
@Data
public class JournalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "actions_seq_gen")
    @SequenceGenerator(name = "actions_seq_gen")
    private Long id;

    @Column(unique = true)
    private String extId;

    private String name;

    private String metaRecord;

    private String typeRef;

    private String predicate;

    private boolean editable;

    private String attributes;

    private String columns;

    private String actions;

}
