package ru.citeck.ecos.uiserv.domain.dashdoard.dto;

import lombok.Data;
import lombok.ToString;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

@Data
@IncludeNonDefault
@ToString(exclude = { "config" })
public class DashboardDto {

    private String id;
    private MLText name;
    private EntityRef typeRef;
    private EntityRef appliedToRef;
    private String authority;
    private String scope = "";
    private float priority;

    private ObjectData config = ObjectData.create();
    private ObjectData attributes = ObjectData.create();

    public DashboardDto() {
    }

    public DashboardDto(DashboardDto other) {

        this.id = other.id;
        this.name = other.name;
        this.typeRef = other.typeRef;
        this.authority = other.authority;
        this.priority = other.priority;
        this.scope = other.scope;
        this.appliedToRef = other.appliedToRef;

        this.config = ObjectData.deepCopy(other.config);
        this.attributes = ObjectData.deepCopy(other.attributes);
    }
}
