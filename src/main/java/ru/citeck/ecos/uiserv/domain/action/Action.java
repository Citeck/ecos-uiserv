package ru.citeck.ecos.uiserv.domain.action;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.citeck.ecos.uiserv.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Table(name = "actions")
public class Action extends BaseEntity {

    private String type;
    private String key;

    @Column(columnDefinition = "TEXT")
    private String configJSON;

    @Column(columnDefinition = "TEXT")
    private String evaluatorJSON;

}
