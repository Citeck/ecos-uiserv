package ru.citeck.ecos.uiserv.domain.action.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import lombok.Data;
import ru.citeck.ecos.uiserv.service.Evaluated;

/**
 * @author Roman Makarskiy
 */
@Data
public class EvaluatorDTO implements Evaluated<JsonNode> {

    private String id;
    private JsonNode config = NullNode.getInstance();

}
