package ru.citeck.ecos.uiserv.domain.config.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import lombok.Data;
import ru.citeck.ecos.uiserv.app.common.service.EntityDto;

/**
 * @author Roman Makarskiy
 */
@Data
public class ConfigDto implements EntityDto {

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

    @Override
    public String getUser() {
        return null;
    }
}
