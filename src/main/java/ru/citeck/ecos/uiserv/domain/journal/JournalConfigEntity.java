package ru.citeck.ecos.uiserv.domain.journal;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "journal_config")
@Data
public class JournalConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "actions_seq_gen")
    @SequenceGenerator(name = "actions_seq_gen")
    private Long id;

    private String type;

    private String config;
}
