package ru.citeck.ecos.uiserv.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import lombok.Data;

/**
 * @author Roman Makarskiy
 */
@Data
public class DashboardDTO {

    private String id;
    private String key;
    private JsonNode config = NullNode.getInstance();

}
