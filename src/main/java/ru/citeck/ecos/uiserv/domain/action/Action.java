package ru.citeck.ecos.uiserv.domain.action;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.citeck.ecos.uiserv.domain.BaseEntity;

import javax.persistence.*;

/**
 * @author Roman Makarskiy
 */
@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Table(name = "actions")
public class Action extends BaseEntity {

    private String type;
    private String icon;

    @Lob
    @Column
    private String configJSON;

    @ManyToOne(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER)
    @JoinColumn(name = "evaluator_id")
    private Evaluator evaluator;

}
