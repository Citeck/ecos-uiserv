package ru.citeck.ecos.uiserv.domain;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity {

    @Id
    protected String id;

    public BaseEntity(String id) {
        this.id = id;
    }

    public BaseEntity() {
        this.id = UUID.randomUUID().toString();
    }

}
