package ru.citeck.ecos.uiserv.domain.action;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.citeck.ecos.uiserv.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * @author Roman Makarskiy
 */
@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Table(name = "evaluators")
public class Evaluator extends BaseEntity {

    @Lob
    @Column
    private String configJSON;

}
