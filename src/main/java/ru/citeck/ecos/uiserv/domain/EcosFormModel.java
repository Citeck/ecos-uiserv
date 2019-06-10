package ru.citeck.ecos.uiserv.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;

public class EcosFormModel {

    @Getter @Setter private String id;
    @Getter @Setter private String title;
    @Getter @Setter private String description;
    @Getter @Setter private String formKey;
    @Getter @Setter private String customModule;
    @Getter @Setter private JsonNode definition;
    @Getter @Setter private ObjectNode i18n;

    public EcosFormModel() {
    }

    public EcosFormModel(EcosFormModel model) {
        this.id = model.getId();
        this.title = model.getTitle();
        this.description = model.getDescription();
        this.formKey = model.getFormKey();
        this.customModule = model.getCustomModule();
        this.definition = model.getDefinition();
        this.i18n = model.getI18n();
    }
}

