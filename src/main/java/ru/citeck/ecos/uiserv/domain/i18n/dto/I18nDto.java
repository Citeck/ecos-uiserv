package ru.citeck.ecos.uiserv.domain.i18n.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import ru.citeck.ecos.commons.data.DataValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class I18nDto {

    private String id;
    private float order;
    private List<String> locales;
    private Map<String, List<String>> messages;

    public I18nDto(I18nDto other) {
        this.id = other.id;
        this.order = other.order;
        this.locales = DataValue.create(other.locales).asStrList();
        this.messages = new HashMap<>();
        if (other.messages != null) {
            other.messages.forEach((k, v) -> this.messages.put(k, DataValue.create(v).asStrList()));
        }
    }
}
