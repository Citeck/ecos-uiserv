package ru.citeck.ecos.uiserv.service.action;

import lombok.Data;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorDto;

@Data
public class ActionModule {
    private String id;
    private String name;
    private String type;
    private String key;
    private String icon;
    private ObjectData config = new ObjectData();
    private RecordEvaluatorDto evaluator;
    private ObjectData attributes;
}
