package ru.citeck.ecos.uiserv.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import lombok.Data;

/**
 * @author Roman Makarskiy
 */
@Data
public class DashboardDto implements EntityDto {

    private String id;
    private String key;
    private String type;
    private String user;
    private JsonNode config = NullNode.getInstance();

    public DashboardDto() {
    }

    public DashboardDto(DashboardDto other) {
        this.id = other.id;
        this.key = other.key;
        this.type = other.type;
        this.user = other.user;
        this.config = other.config;
    }
}
