package ru.citeck.ecos.uiserv.domain.journal;

import lombok.Data;
import ru.citeck.ecos.commons.data.MLText;

import javax.persistence.*;

@Entity
@Table(name = "journal_column")
@Data
public class JournalColumnEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "actions_seq_gen")
    @SequenceGenerator(name = "actions_seq_gen")
    private Long id;

    private String attribute;

    private String editorRef;

    private String type;

    private boolean searchable;

    private boolean sortable;

    private boolean groupable;

    private boolean editable;

    private String name;

    private boolean show;

    private boolean visible;

    private String attributes;

    @OneToOne(fetch = FetchType.EAGER,cascade = CascadeType.ALL)
    @JoinColumn(name = "journal_formatter_config_id")
    private JournalConfigEntity formatter;

    @OneToOne(fetch = FetchType.EAGER,cascade = CascadeType.ALL)
    @JoinColumn(name = "journal_options_config_id")
    private JournalConfigEntity options;

    @OneToOne(fetch = FetchType.EAGER,cascade = CascadeType.ALL)
    @JoinColumn(name = "journal_filter_config_id")
    private JournalConfigEntity filter;
}
