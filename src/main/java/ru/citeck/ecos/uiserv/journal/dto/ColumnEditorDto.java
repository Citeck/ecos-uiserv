package ru.citeck.ecos.uiserv.journal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.citeck.ecos.commons.data.ObjectData;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColumnEditorDto {
    private String type;
    private ObjectData config;
    private boolean batchEdit;
}
