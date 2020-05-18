package ru.citeck.ecos.uiserv.service.menu.dto;

import lombok.Data;
import ru.citeck.ecos.commons.data.ObjectData;

@Data
public class MenuItemActionDto {
    private String type;
    private ObjectData config;
}
