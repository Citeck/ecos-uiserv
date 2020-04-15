package ru.citeck.ecos.uiserv.domain.journal;

import lombok.Data;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.uiserv.domain.ActionEntity;

import javax.persistence.*;
import java.util.HashSet;
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

    private byte[] predicate;

    private boolean editable;

    private String attributes;

    @ManyToMany(
        fetch = FetchType.EAGER,
        cascade = CascadeType.ALL)
    @JoinTable(
        name = "journal_action",
        joinColumns = @JoinColumn(name = "journal_id"),
        inverseJoinColumns = @JoinColumn(name = "action_id")
    )
    private Set<ActionEntity> actions = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "journal_id")
    private Set<JournalColumnEntity> columns = new HashSet<>();

}
