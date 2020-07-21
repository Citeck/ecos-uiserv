package ru.citeck.ecos.uiserv.domain.journal.api.records.utils;

import lombok.Data;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.RecordRef;

@Data
public class CreateVariantDto {
    private String id;
    private MLText name;
    private RecordRef formRef;
    private RecordRef recordRef;
    private ObjectData attributes = ObjectData.create();
}
