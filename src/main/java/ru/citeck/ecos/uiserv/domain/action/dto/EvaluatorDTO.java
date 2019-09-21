package ru.citeck.ecos.uiserv.domain.action.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import lombok.Data;

/**
 * @author Roman Makarskiy
 */
@Data
public class EvaluatorDTO {

    private String id;
    private JsonNode config = NullNode.getInstance();

}
