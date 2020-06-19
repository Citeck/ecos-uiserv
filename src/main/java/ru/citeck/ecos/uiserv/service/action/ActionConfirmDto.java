package ru.citeck.ecos.uiserv.service.action;

import lombok.Data;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.records2.RecordRef;

import java.util.Map;

@Data
public class ActionConfirmDto {
    private MLText title;
    private MLText message;
    private RecordRef formRef;
    private Map<String, String> attributesMapping;
}
