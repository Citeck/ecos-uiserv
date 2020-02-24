package ru.citeck.ecos.uiserv.domain;

import lombok.Data;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.commons.data.ObjectData;

@Data
public class DashboardDto {

    private String id;
    private RecordRef typeRef;
    private String authority;
    private ObjectData config;
    private float priority;

    public DashboardDto() {
    }

    public DashboardDto(DashboardDto other) {
        this.id = other.id;
        this.typeRef = other.typeRef;
        this.authority = other.authority;
        this.config = other.config;
        this.priority = other.priority;
    }
}
