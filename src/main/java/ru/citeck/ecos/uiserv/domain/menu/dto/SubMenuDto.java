package ru.citeck.ecos.uiserv.domain.menu.dto;

import ecos.com.fasterxml.jackson210.annotation.JsonInclude;
import lombok.Data;
import ru.citeck.ecos.commons.data.ObjectData;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SubMenuDto {

    private ObjectData config;
    private List<MenuItemDto> items;
}
