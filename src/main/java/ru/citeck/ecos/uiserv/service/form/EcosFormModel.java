package ru.citeck.ecos.uiserv.service.form;

import lombok.Data;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.RecordRef;

@Data
public class EcosFormModel {

    private String id;
    private String formKey;
    private MLText title;
    private MLText description;
    private String customModule;
    private RecordRef typeRef;
    private String width;

    private ObjectData i18n = new ObjectData();
    private ObjectData definition = new ObjectData();

    private ObjectData attributes = new ObjectData();

    public EcosFormModel() {
    }

    public EcosFormModel(EcosFormModel model) {

        this.id = model.getId();
        this.width = model.getWidth();
        this.title = model.getTitle();
        this.formKey = model.getFormKey();
        this.typeRef = model.getTypeRef();
        this.description = model.getDescription();
        this.customModule = model.getCustomModule();

        this.i18n = ObjectData.deepCopy(model.getI18n());
        this.definition = ObjectData.deepCopy(model.getDefinition());
        this.attributes = ObjectData.deepCopy(model.getAttributes());
    }
}

