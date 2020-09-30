package ru.citeck.ecos.uiserv.domain.theme.dto;

import lombok.Data;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.MLText;

import java.util.HashMap;
import java.util.Map;

@Data
public class ThemeDto {

    private String id;
    private MLText name;
    private Map<String, String> images = new HashMap<>();
    private Map<String, byte[]> resources = new HashMap<>();

    public ThemeDto() {
    }

    public ThemeDto(ThemeDto other) {
        this.id = other.id;
        this.name = other.name;
        this.images = DataValue.create(other.images).asMap(String.class, String.class);
        this.resources = DataValue.create(other.resources).asMap(String.class, byte[].class);
    }
}
