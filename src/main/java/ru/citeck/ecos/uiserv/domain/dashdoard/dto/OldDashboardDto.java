package ru.citeck.ecos.uiserv.domain.dashdoard.dto;

import lombok.Data;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.uiserv.domain.common.service.EntityDto;

/**
 * @author Roman Makarskiy
 */
@Data
@Deprecated
public class OldDashboardDto implements EntityDto {

    private String id;
    private String key;
    private String type;
    private String user;
    private ObjectData config;

    public OldDashboardDto() {
    }

    public OldDashboardDto(OldDashboardDto other) {
        this.id = other.id;
        this.key = other.key;
        this.type = other.type;
        this.user = other.user;
        this.config = other.config;
    }
}
