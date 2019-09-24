package ru.citeck.ecos.uiserv.service.evaluator;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.exception.RecordsException;

/**
 * @author Roman Makarskiy
 */
@Component("record-permission")
public class RecordPermissionEvaluator implements RecordEvaluator {

    private static final String PERMISSION_ATT_PATTERN = ".att(n:\"permissions\"){has(n:\"%s\")}";

    private final RecordsService recordsService;

    @Autowired
    public RecordPermissionEvaluator(RecordsService recordsService) {
        this.recordsService = recordsService;
    }

    @Override
    public boolean evaluate(Object config, RecordRef record) {
        String permission;

        if (config instanceof JsonNode) {
            JsonNode permissionNode = ((JsonNode) config).get("permission");
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
