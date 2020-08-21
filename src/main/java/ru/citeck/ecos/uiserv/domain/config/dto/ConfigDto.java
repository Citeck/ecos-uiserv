package ru.citeck.ecos.uiserv.domain.config.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import ecos.com.fasterxml.jackson210.annotation.JsonInclude;
import lombok.Data;
import ru.citeck.ecos.uiserv.app.common.service.EntityDto;

/**
 * @author Roman Makarskiy
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_DEFAULT)
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