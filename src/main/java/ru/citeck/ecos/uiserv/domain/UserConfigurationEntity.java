package ru.citeck.ecos.uiserv.domain;

import lombok.Data;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;

@Data
@Entity
@Immutable
@Table(name = "user_configuration")
public class UserConfigurationEntity implements Serializable {
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
