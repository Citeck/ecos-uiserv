package ru.citeck.ecos.uiserv.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.citeck.ecos.uiserv.web.rest.menu.xml.MenuConfig;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Getter
@Setter
public class MenuConfigurationDto {
    private String id;
    private String type;
    private String authorities;
    private MenuConfig config;
    private Integer modelVersion = 0;
    private LocalizationMap localization = new LocalizationMap();

    public String getLocalizedString(String stringId, Locale locale) {
        Map<String, String> localizedStringsMap = localization.get(locale);

        return localizedStringsMap != null ? localizedStringsMap.get(stringId) : null;
    }

    @NoArgsConstructor
    public static class LocalizationMap extends HashMap<Locale, Map<String, String>> {
        public LocalizationMap(Map<? extends Locale, ? extends Map<String, String>> m) {
            super(m);
        }
    }
}
