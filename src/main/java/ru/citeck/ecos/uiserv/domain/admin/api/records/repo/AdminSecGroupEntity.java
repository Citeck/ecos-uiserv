package ru.citeck.ecos.uiserv.domain.admin.api.records.repo;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "admin_sections_group")
public class AdminSecGroupEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @Column(name = "ext_id")
    private String externalId;

    @Column(name = "name")
    private String name;

    @Column(name = "\"order\"")
    private float order;

    @Column(name = "sections")
    private String sections;
}
