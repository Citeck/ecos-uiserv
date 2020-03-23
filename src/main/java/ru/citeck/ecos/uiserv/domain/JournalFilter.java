package ru.citeck.ecos.uiserv.domain;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;

@Data
@Entity
@Table(name = "journal_filter")
public class JournalFilter implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @Column(name = "ext_id")
    private String externalId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "creation_time")
    private Instant creationTime;

    @Column
    private String data;
}
