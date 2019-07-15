package ru.citeck.ecos.uiserv.service.form;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;

public class EcosFormModel {

    @Getter @Setter private String id;
    @Getter @Setter private String title;
    @Getter @Setter private String formKey;
    @Getter @Setter private String formMode;
    @Getter @Setter private ObjectNode i18n;
    @Getter @Setter private String description;
    @Getter @Setter private String customModule;
    @Getter @Setter private JsonNode definition;

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

