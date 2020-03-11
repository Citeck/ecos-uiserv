package ru.citeck.ecos.uiserv.service.form;

import lombok.Data;
import ru.citeck.ecos.commons.data.ObjectData;

@Data
public class FormModule {

    private String id;
    private String title;
    private String formKey;
    private String formMode;
    private String description;
    private String customModule;

    private ObjectData i18n;
    private ObjectData definition;

    private ObjectData attributes;
}
