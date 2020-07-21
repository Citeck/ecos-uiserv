package ru.citeck.ecos.uiserv.domain.action.dto;

import ecos.com.fasterxml.jackson210.annotation.JsonInclude;
import lombok.Data;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorDto;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ActionModule {

    private String id;
    private MLText name;
    private String key;
    private String icon;
    private RecordRef typeRef;

    private ActionConfirmDto confirm;
    private ActionResultDto result;

    private String type;
    private ObjectData config = ObjectData.create();

    private RecordEvaluatorDto evaluator;
    private ObjectData attributes = ObjectData.create();

    public ActionModule() {
    }

    public ActionModule(ActionModule other) {

        this.id = other.id;
        this.name = other.name;
        this.type = other.type;
        this.key = other.key;
        this.icon = other.icon;
        this.confirm = Json.getMapper().copy(other.confirm);
        this.result = Json.getMapper().copy(other.result);
        this.evaluator = other.evaluator;
        this.typeRef = other.typeRef;

        this.config = ObjectData.deepCopy(other.config);
        this.attributes = ObjectData.deepCopy(other.attributes);
    }
}
