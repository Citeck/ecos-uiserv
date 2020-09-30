package ru.citeck.ecos.uiserv.domain.theme.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResourceData {
    private final String fileName;
    private final byte[] data;
}
