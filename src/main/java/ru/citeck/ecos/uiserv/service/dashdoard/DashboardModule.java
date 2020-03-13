package ru.citeck.ecos.uiserv.service.dashdoard;

import lombok.Data;
import ru.citeck.ecos.apps.module.ModuleRef;
import ru.citeck.ecos.commons.data.ObjectData;

@Data
public class DashboardModule {

    private String id;
    private String authority;
    private ModuleRef typeRef;
    private float priority;
    private ObjectData config;
    private ObjectData attributes;
}
