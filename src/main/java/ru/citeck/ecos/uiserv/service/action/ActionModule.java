package ru.citeck.ecos.uiserv.service.action;

import lombok.Data;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorDto;

@Data
public class ActionModule {

    private String id;
    private MLText name;
    private String key;
    private String icon;
    private RecordRef typeRef;

    private String type;
    private ObjectData config = new ObjectData();

    private RecordEvaluatorDto evaluator;
    private ObjectData attributes = new ObjectData();

    public ActionModule() {
    }

    public ActionModule(ActionModule other) {

        this.id = other.id;
        this.name = other.name;
        this.type = other.type;
        this.key = other.key;
        this.icon = other.icon;
        this.evaluator = other.evaluator;
        this.typeRef = other.typeRef;

        this.config = ObjectData.deepCopy(other.config);
        this.attributes = ObjectData.deepCopy(other.attributes);
    }
}
