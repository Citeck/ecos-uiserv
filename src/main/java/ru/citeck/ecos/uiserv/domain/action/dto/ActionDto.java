package ru.citeck.ecos.uiserv.domain.action.dto;

import ecos.com.fasterxml.jackson210.annotation.JsonInclude;
import lombok.Data;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorDto;

import java.util.HashMap;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ActionDto {

    private String id;

    private MLText name;
    private MLText pluralName;

    private String icon;
    private RecordRef typeRef;

    private ActionConfirmDto confirm;
    private ActionResultDto result;

    private String type;
    private ObjectData config = ObjectData.create();

    private RecordEvaluatorDto evaluator;
    private ObjectData attributes = ObjectData.create();

    private Map<String, Boolean> features = new HashMap<>();

    public ActionDto() {
    }

    public ActionDto(ActionDto other) {

        this.id = other.id;
        this.name = other.name;
        this.pluralName = other.pluralName;
        this.type = other.type;
        this.icon = other.icon;
        this.confirm = Json.getMapper().copy(other.confirm);
        this.result = Json.getMapper().copy(other.result);
        this.evaluator = other.evaluator;
        this.typeRef = other.typeRef;

        this.config = ObjectData.deepCopy(other.config);
        this.attributes = ObjectData.deepCopy(other.attributes);
        this.features = DataValue.create(features).asMap(String.class, Boolean.class);
    }
}
