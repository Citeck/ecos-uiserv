package ru.citeck.ecos.uiserv.service.form;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;

@Data
public class EcosFormModel {

    private String id;
    private String title;
    private String formKey;
    private String formMode;
    private ObjectNode i18n;
    private String description;
    private String customModule;
    private JsonNode definition;

    public EcosFormModel() {
    }

    public EcosFormModel(EcosFormModel model) {
        this.id = model.getId();
        this.i18n = model.getI18n();
        this.title = model.getTitle();
        this.formKey = model.getFormKey();
        this.formMode = model.getFormMode();
        this.definition = model.getDefinition();
        this.description = model.getDescription();
        this.customModule = model.getCustomModule();
    }
}

