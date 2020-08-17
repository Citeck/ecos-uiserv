package ru.citeck.ecos.uiserv.domain.journal.dto;

import lombok.Data;
import ru.citeck.ecos.commons.data.ObjectData;

@Data
public class ComputedParamDto {
    private String name;
    private String type;
    private ObjectData config;
}
