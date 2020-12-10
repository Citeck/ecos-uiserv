package ru.citeck.ecos.uiserv.domain.journal.dto;

import lombok.Data;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.MLText;

@Data
public class ColumnOptionDto {
    private MLText label;
    private DataValue value;
}
