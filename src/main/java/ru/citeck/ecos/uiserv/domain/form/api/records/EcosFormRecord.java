package ru.citeck.ecos.uiserv.domain.form.api.records;

import ecos.com.fasterxml.jackson210.annotation.JsonIgnore;
import ecos.com.fasterxml.jackson210.annotation.JsonProperty;
import ecos.com.fasterxml.jackson210.annotation.JsonValue;
import org.apache.commons.lang.StringUtils;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.json.YamlUtils;
import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
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

    @AttName(".disp")
    public String getDisplayName() {
        String name = MLText.getClosestValue(getTitle(), QueryContext.getCurrent().getLocale());
        if (StringUtils.isBlank(name)) {
            name = "Form";
        }
        return name;
    }

    @JsonIgnore
    public String get_formKey() {
        return "module_form";
    }

    @JsonProperty("_content")
    public void setContent(List<ObjectData> content) {

        String dataUriContent = content.get(0).get("url", "");
        ObjectData data = Json.getMapper().read(dataUriContent, ObjectData.class);

        Json.getMapper().applyData(this, data);
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
