package ru.citeck.ecos.uiserv.domain.userconfig.repo;

import lombok.Data;
import org.hibernate.annotations.Immutable;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;

@Data
@Entity
@Immutable
@Table(name = "user_configuration")
public class UserConfigurationEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence")
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
