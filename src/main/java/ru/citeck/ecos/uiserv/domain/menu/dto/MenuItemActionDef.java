package ru.citeck.ecos.uiserv.domain.menu.dto;

import lombok.Data;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault;

@Data
@IncludeNonDefault
public class MenuItemActionDef {
    private String type;
    private ObjectData config;
}
