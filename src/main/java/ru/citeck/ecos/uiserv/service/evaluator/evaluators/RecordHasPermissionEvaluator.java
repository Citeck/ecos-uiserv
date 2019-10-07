package ru.citeck.ecos.uiserv.service.evaluator.evaluators;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.exception.RecordsException;
import ru.citeck.ecos.uiserv.service.evaluator.RecordEvaluator;

/**
 * @author Roman Makarskiy
 */
@Component("record-has-permission")
public class RecordHasPermissionEvaluator implements RecordEvaluator {

    private static final String PERMISSION_ATT_PATTERN = ".att(n:\"permissions\"){has(n:\"%s\")}";
    private static final String PERMISSION_CONFIG_ATT = "permission";

    private final RecordsService recordsService;

    @Autowired
    public RecordHasPermissionEvaluator(RecordsService recordsService) {
        this.recordsService = recordsService;
    }

    @Override
    public boolean evaluate(Object config, RecordRef record) {
        String permission;

        if (config instanceof JsonNode) {
            JsonNode permissionNode = ((JsonNode) config).get(PERMISSION_CONFIG_ATT);
            if (permissionNode == null || permissionNode.isNull() || permissionNode.isMissingNode()) {
                throw new IllegalArgumentException("You need to specify a permission kind, for evaluating. Config:"
                    + config.toString());
            }

            permission = permissionNode.asText();
        } else {
            throw new IllegalArgumentException("Unsupported format of config");
        }

        String permissionAttrSchema = String.format(PERMISSION_ATT_PATTERN, permission);
        JsonNode attribute = recordsService.getAttribute(record, permissionAttrSchema);
        if (attribute == null || attribute.isNull() || attribute.isMissingNode()) {
            throw new RecordsException(String.format("Failed get permission attribute from record <%s>," +
                " attribute schema: <%s>", record, permissionAttrSchema));
        }

        return attribute.asBoolean();
    }

}
