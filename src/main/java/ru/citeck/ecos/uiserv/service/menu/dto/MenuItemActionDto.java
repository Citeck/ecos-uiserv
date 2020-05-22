package ru.citeck.ecos.uiserv.service.menu.dto;

import ecos.com.fasterxml.jackson210.annotation.JsonInclude;
import lombok.Data;
import ru.citeck.ecos.commons.data.ObjectData;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MenuItemActionDto {
    private String type;
    private ObjectData config;
}
