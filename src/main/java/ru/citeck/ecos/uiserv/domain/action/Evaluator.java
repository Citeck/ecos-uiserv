package ru.citeck.ecos.uiserv.domain.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import lombok.Data;

@Data
public class Evaluator {

    private String id;
    private JsonNode config = NullNode.getInstance();

}
