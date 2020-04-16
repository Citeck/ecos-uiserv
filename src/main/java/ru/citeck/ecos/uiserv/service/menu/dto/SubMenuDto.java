package ru.citeck.ecos.uiserv.service.menu.dto;

import lombok.Data;
import ru.citeck.ecos.commons.data.ObjectData;

import java.util.List;

@Data
public class SubMenuDto {

    private ObjectData config;
    private List<MenuItemDto> items;
}
