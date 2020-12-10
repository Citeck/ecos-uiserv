package ru.citeck.ecos.uiserv.domain.journal.dto;

import lombok.Data;
import ru.citeck.ecos.commons.data.ObjectData;

@Data
public class ColumnEditorDto {
    private String type;
    private ObjectData config = ObjectData.create();
}
