package ru.citeck.ecos.uiserv.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import lombok.Data;

/**
 * @author Roman Makarskiy
 */
@Data
public class ConfigDTO implements EntityDTO {

    private String id;
    private String title;
    private String description;
    private JsonNode value = NullNode.getInstance();

    @Override
    public String getKey() {
        return null;
    }

    @Override
    public String getType() {
        return null;
    }
}
