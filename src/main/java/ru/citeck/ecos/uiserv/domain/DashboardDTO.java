package ru.citeck.ecos.uiserv.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import lombok.Data;

/**
 * @author Roman Makarskiy
 */
@Data
public class DashboardDTO implements EntityDTO {

    private String id;
    private String key;
    private String type;
    private String user;
    private JsonNode config = NullNode.getInstance();
}
