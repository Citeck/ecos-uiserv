package ru.citeck.ecos.uiserv.domain.dashdoard.dto;

import ecos.com.fasterxml.jackson210.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.commons.data.ObjectData;

@Data
@JsonInclude(value = JsonInclude.Include.NON_DEFAULT)
@ToString(exclude = { "config" })
public class DashboardDto {

    private String id;
    private RecordRef typeRef;
    private String authority;
    private float priority;

    private ObjectData config = ObjectData.create();
    private ObjectData attributes = ObjectData.create();

    public DashboardDto() {
    }

    public DashboardDto(DashboardDto other) {

        this.id = other.id;
        this.typeRef = other.typeRef;
        this.authority = other.authority;
        this.priority = other.priority;

        this.config = ObjectData.deepCopy(other.config);
        this.attributes = ObjectData.deepCopy(other.attributes);
    }
}
