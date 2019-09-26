package ru.citeck.ecos.uiserv.domain.action;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

/**
 * @author Roman Makarskiy
 */
@Data
@Entity
@EqualsAndHashCode
@Table(name = "actions")
public class Action {

    @Id
    private String id;

    private String title;
    private String type;
    private String icon;

    @Lob
    @Column(name = "config_json")
    private String configJSON;

    @OneToOne(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "evaluator")
    private Evaluator evaluator;

}
