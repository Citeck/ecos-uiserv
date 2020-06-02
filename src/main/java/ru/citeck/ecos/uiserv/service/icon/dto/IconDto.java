package ru.citeck.ecos.uiserv.service.icon.dto;

import lombok.Data;
import lombok.ToString;
import ru.citeck.ecos.commons.data.ObjectData;

import java.time.Instant;

@Data
@ToString(exclude = {"data"})
public class IconDto {

    private String id;
    private String family;

    private String type;
    private ObjectData config;
    private byte[] data;

    private Instant modified;
}
