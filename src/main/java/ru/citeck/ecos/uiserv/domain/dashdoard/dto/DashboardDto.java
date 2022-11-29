package ru.citeck.ecos.uiserv.domain.dashdoard.dto;

import lombok.Data;
import lombok.ToString;
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.commons.data.ObjectData;

@Data
@IncludeNonDefault
@ToString(exclude = { "config" })
public class DashboardDto {

    private String id;
    private RecordRef typeRef;
    private RecordRef appliedToRef;
    private String authority;
    private String scope = "";
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
        this.scope = other.scope;
        this.appliedToRef = other.appliedToRef;

        this.config = ObjectData.deepCopy(other.config);
        this.attributes = ObjectData.deepCopy(other.attributes);
    }
}
