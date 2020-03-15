package ru.citeck.ecos.uiserv.domain;

import lombok.Data;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.commons.data.ObjectData;

@Data
public class DashboardDto {

    private String id;
    private RecordRef typeRef = RecordRef.EMPTY;
    private String authority;
    private float priority;

    private ObjectData config = new ObjectData();
    private ObjectData attributes = new ObjectData();

    public DashboardDto() {
    }

    public DashboardDto(DashboardDto other) {

        this.id = other.id;
        this.typeRef = other.typeRef;
        this.authority = other.authority;
        this.priority = other.priority;

        this.config = other.config.deepCopy();
        this.attributes = other.attributes.deepCopy();
    }
}
