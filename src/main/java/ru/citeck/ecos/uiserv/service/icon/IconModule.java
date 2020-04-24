package ru.citeck.ecos.uiserv.service.icon;

import lombok.Data;
import ru.citeck.ecos.uiserv.service.icon.dto.IconType;

@Data
public class IconModule {
    private String id;
    private IconType type;
    private String format;
    private String data;
}
