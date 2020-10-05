package ru.citeck.ecos.uiserv.domain.journal.repo;

import lombok.Data;
import ru.citeck.ecos.uiserv.app.common.repo.AbstractAuditingEntity;

import javax.persistence.*;

@Entity
@Table(name = "journal_settings")
@Data
public class JournalSettingsEntity extends AbstractAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "journal_settings_seq_gen")
    @SequenceGenerator(name = "journal_settings_seq_gen")
    private Long id;

    @Column(unique = true)
    private String extId;

    private String name;

    private String journalId;

    private String authority;

    private String settings;
}
