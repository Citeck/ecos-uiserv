package ru.citeck.ecos.uiserv.service.icon.dto;

import lombok.Data;
import lombok.ToString;

import java.time.Instant;

@Data
@ToString(exclude = {"data"})
public class IconDto {
    private String id;
    private String type;
    private String format;
    private String data;
    private Instant modified;
}
