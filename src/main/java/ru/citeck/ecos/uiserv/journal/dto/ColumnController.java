package ru.citeck.ecos.uiserv.journal.dto;

import lombok.Data;
import ru.citeck.ecos.commons.data.ObjectData;

@Data
public class ColumnController {
    private String type;
    private ObjectData config;
}
