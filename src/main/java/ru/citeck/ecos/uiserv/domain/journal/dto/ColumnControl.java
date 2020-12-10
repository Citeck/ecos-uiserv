package ru.citeck.ecos.uiserv.domain.journal.dto;

import lombok.Data;
import ru.citeck.ecos.commons.data.ObjectData;

@Data
@Deprecated
public class ColumnControl {
    private String type;
    private ObjectData config = ObjectData.create();
}
