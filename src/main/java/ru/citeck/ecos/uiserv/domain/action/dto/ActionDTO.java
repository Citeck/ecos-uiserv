package ru.citeck.ecos.uiserv.domain.action.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import lombok.Data;
import ru.citeck.ecos.uiserv.domain.EntityDTO;

/**
 * @author Roman Makarskiy
 */
@Data
public class ActionDTO implements EntityDTO {

    private String id;
    private String type;
    private String icon;
    private JsonNode config = NullNode.getInstance();
    private EvaluatorDTO evaluator;

    @Override
    public String getKey() {
        return null;
    }
}
