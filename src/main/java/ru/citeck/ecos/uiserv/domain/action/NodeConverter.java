package ru.citeck.ecos.uiserv.domain.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;

/**
 * @author Roman Makarskiy
 */
class NodeConverter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static String nodeAsString(JsonNode node) {
        try {
            return OBJECT_MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed write JsonNode as String", e);
        }
    }

    static JsonNode fromString(String nodeData) {
        if (StringUtils.isBlank(nodeData)) {
            return NullNode.getInstance();
        }

        try {
            return OBJECT_MAPPER.readValue(nodeData, JsonNode.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed read JSON", e);
        }
    }

}
