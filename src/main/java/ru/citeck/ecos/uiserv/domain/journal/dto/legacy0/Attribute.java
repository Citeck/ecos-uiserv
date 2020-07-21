package ru.citeck.ecos.uiserv.domain.journal.dto.legacy0;

import ecos.com.fasterxml.jackson210.annotation.JsonProperty;
import lombok.Data;
import ru.citeck.ecos.commons.data.DataValue;

import java.util.HashMap;
import java.util.Map;

@Data
public class Attribute {
    private String name;
    @JsonProperty("isDefault")
    private Boolean isDefault;
    private Boolean visible;
    private Boolean searchable;
    private Boolean sortable;
    private Boolean groupable;
    private Map<String, String> settings = new HashMap<>();
    private DataValue batchEdit;
    private DataValue criterionInvariants;
}
