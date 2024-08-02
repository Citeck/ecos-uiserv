package ru.citeck.ecos.uiserv.app.application.api.records;

import kotlin.Unit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.source.dao.local.meta.MetaAttributesSupplier;
import ru.citeck.ecos.records2.source.dao.local.meta.MetaRecordsDaoAttsProvider;
import ru.citeck.ecos.uiserv.domain.ecostype.config.EcosTypesComponent;
import ru.citeck.ecos.uiserv.domain.i18n.service.I18nService;
import ru.citeck.ecos.uiserv.domain.icon.service.IconService;
import ru.citeck.ecos.uiserv.domain.journal.service.JournalService;
import ru.citeck.ecos.uiserv.domain.menu.service.MenuService;
import ru.citeck.ecos.uiserv.domain.theme.service.ThemeService;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class MetaRecordAttsProvider implements MetaAttributesSupplier {

    private static final String ATT_MENU_CACHE_KEY = "menu-cache-key";
    private static final String ATT_I18N_CACHE_KEY = "i18n-cache-key";
    private static final String ATT_THEME_CACHE_KEY = "theme-cache-key";
    private static final String ATT_IMAGES_CACHE_KEY = "images-cache-key";

    private final JournalService journalService;
    private final MenuService menuService;
    private final MetaRecordsDaoAttsProvider provider;
    private final I18nService i18nService;
    private final ThemeService themeService;
    private final IconService iconService;
    private final EcosTypesComponent ecosTypesComponent;

    private String typesLastModified = "";

    @PostConstruct
    void init() {
        provider.register(this);

        ecosTypesComponent.addOnTypeChangedListener(type -> {
            typesLastModified = "" + Instant.now().toEpochMilli();
            return Unit.INSTANCE;
        });
    }

    @Override
    public List<String> getAttributesList() {
        return Arrays.asList(
            ATT_MENU_CACHE_KEY,
            ATT_I18N_CACHE_KEY,
            ATT_THEME_CACHE_KEY,
            ATT_IMAGES_CACHE_KEY
        );
    }

    @Override
    public Object getAttribute(String attribute) {

        switch (attribute) {

            case ATT_MENU_CACHE_KEY:
                return Objects.hash(
                    journalService.getLastModifiedTimeMs(),
                    menuService.getLastModifiedTimeMs(),
                    i18nService.getCacheKey(),
                    typesLastModified
                );

            case ATT_I18N_CACHE_KEY:

                return i18nService.getCacheKey();

            case ATT_THEME_CACHE_KEY:

                return Objects.hash(
                    themeService.getCacheKey(),
                    iconService.getCacheKey()
                );

            case ATT_IMAGES_CACHE_KEY:

                return iconService.getCacheKey();
        }

        return null;
    }
}
