package ru.citeck.ecos.uiserv.service.icon.dto;

import ecos.com.fasterxml.jackson210.annotation.JsonValue;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum IconType {
    FA("fa"),
    IMG("img");

    private static final IconType[] TYPES = IconType.values();
    private final String typeString;

    public static IconType byTypeString(String typeString) {
        for (IconType type : TYPES) {
            if (type.typeString.equalsIgnoreCase(typeString)) {
                return type;
            }
        }
        return null;
    }

    @JsonValue
    public String getTypeString() {
        return typeString;
    }
}
