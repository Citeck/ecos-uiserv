package ru.citeck.ecos.uiserv.domain.form.dto;

import lombok.Data;
import lombok.ToString;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.RecordRef;

@Data
@ToString(exclude = { "definition", "i18n" })
public class EcosFormModel {

    private String id;
    private String formKey;
    private MLText title;
    private MLText description;
    private String customModule;
    private RecordRef typeRef = RecordRef.EMPTY;
    private String width;

    private ObjectData i18n = ObjectData.create();
    private ObjectData definition = ObjectData.create();

    private ObjectData attributes = ObjectData.create();

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

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public RecordRef getTypeRef() {
        return typeRef;
    }

    public void setDefinition(ObjectData definition) {
        this.definition = definition;
    }

    public ObjectData getDefinition() {
        return definition;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

