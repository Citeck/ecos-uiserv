package ru.citeck.ecos.uiserv.service.evaluator.evaluators;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.exception.RecordsException;
import ru.citeck.ecos.uiserv.service.evaluator.RecordEvaluator;

/**
 * @author Roman Makarskiy
 */
@Component("record-has-attribute")
public class RecordHasAttributeEvaluator implements RecordEvaluator {

    private static final String HAS_ATTRIBUTE_PATTERN = ".has(n:\"%s\")";

    private final RecordsService recordsService;

    public RecordHasAttributeEvaluator(RecordsService recordsService) {
        this.recordsService = recordsService;
    }

    @Override
    public boolean evaluate(Object config, RecordRef record) {
        String attribute;

        if (config instanceof JsonNode) {
            JsonNode attributeNode = ((JsonNode) config).get("attribute");
            if (attributeNode == null || attributeNode.isNull() || attributeNode.isMissingNode()) {
                throw new IllegalArgumentException("You need to specify a attribute, for evaluating. Config:"
                    + config.toString());
            }

            attribute = attributeNode.asText();
        } else {
            throw new IllegalArgumentException("Unsupported format of config");
        }

        String attrSchema = String.format(HAS_ATTRIBUTE_PATTERN, attribute);
        JsonNode result = recordsService.getAttribute(record, attrSchema);
        if (result == null || result.isNull() || result.isMissingNode()) {
            throw new RecordsException(String.format("Failed get attribute from record <%s>," +
                " attribute schema: <%s>", record, attrSchema));
        }

        return result.asBoolean();
    }
}
