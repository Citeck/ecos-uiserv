package ru.citeck.ecos.uiserv.service.menu;

import lombok.Data;

@Data
public class MenuDeployModule {
    private String id;
    private String filename;
    private byte[] data;
}
