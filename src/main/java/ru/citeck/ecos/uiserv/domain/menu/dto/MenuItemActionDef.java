package ru.citeck.ecos.uiserv.domain.menu.dto;

import ecos.com.fasterxml.jackson210.annotation.JsonInclude;
import lombok.Data;
import ru.citeck.ecos.commons.data.ObjectData;

@Data
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class MenuItemActionDef {
    private String type;
    private ObjectData config;
}
