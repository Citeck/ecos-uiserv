package ru.citeck.ecos.uiserv.service.i18n;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class I18nDto {

    private String id;
    private float order;
    private List<String> locales;
    private Map<String, List<String>> messages;
}
