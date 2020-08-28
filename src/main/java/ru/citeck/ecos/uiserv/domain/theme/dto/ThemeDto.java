package ru.citeck.ecos.uiserv.domain.theme.dto;

import lombok.Data;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.records2.RecordRef;

import java.util.HashMap;
import java.util.Map;

@Data
public class ThemeDto {

    private String id;
    private MLText name;
    private Map<String, RecordRef> images = new HashMap<>();
    private Map<String, byte[]> styles = new HashMap<>();

    public ThemeDto() {
    }

    public ThemeDto(ThemeDto other) {
        this.id = other.id;
        this.name = other.name;
        this.images = DataValue.create(other.images).asMap(String.class, RecordRef.class);
        this.styles = DataValue.create(other.styles).asMap(String.class, byte[].class);
    }
}
