package ru.citeck.ecos.uiserv.domain.form.api.records;

import ecos.com.fasterxml.jackson210.annotation.JsonProperty;
import ecos.com.fasterxml.jackson210.annotation.JsonValue;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.json.YamlUtils;
import ru.citeck.ecos.model.lib.type.service.utils.TypeUtils;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormModel;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class EcosFormRecord extends EcosFormModel {

    public EcosFormRecord(EcosFormModel model) {
        super(model);
    }

    public String getModuleId() {
        return getId();
    }

    public void setModuleId(String value) {
        setId(value);
    }

    public MLText getDisplayName() {
        MLText title = getTitle();
        if (MLText.isEmpty(title)) {
            return new MLText("Form");
        }
        return title;
    }

    @JsonProperty("_content")
    public void setContent(List<ObjectData> content) {

        String dataUriContent = content.get(0).get("url", "");
        ObjectData data = Json.getMapper().read(dataUriContent, ObjectData.class);

        Json.getMapper().applyData(this, data);
    }

    public String getEcosType() {
        return "form";
    }

    @JsonValue
    @com.fasterxml.jackson.annotation.JsonValue
    public EcosFormModel toJson() {
        return new EcosFormModel(this);
    }

    public byte[] getData() {
        return YamlUtils.toNonDefaultString(toJson()).getBytes(StandardCharsets.UTF_8);
    }
}
