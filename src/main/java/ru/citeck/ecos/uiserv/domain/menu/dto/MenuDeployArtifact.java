package ru.citeck.ecos.uiserv.domain.menu.dto;

import lombok.Data;

@Data
public class MenuDeployArtifact {
    private String id;
    private String filename;
    private byte[] data;
}
