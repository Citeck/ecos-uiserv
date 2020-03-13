package ru.citeck.ecos.uiserv.service.action;

import lombok.Data;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorDto;

@Data
public class ActionModule {

    private String id;
    private MLText name;
    private String type;
    private String key;
    private String icon;
    private ObjectData config = new ObjectData();
    private RecordEvaluatorDto evaluator;
    private ObjectData attributes;

    public ActionModule() {
    }

    public ActionModule(ActionModule other) {
        this.id = other.id;
        this.name = other.name;
        this.type = other.type;
        this.key = other.key;
        this.icon = other.icon;
        this.config = other.config;
        this.evaluator = other.evaluator;
        this.attributes = other.attributes;
    }
}
