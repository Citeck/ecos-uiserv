package ru.citeck.ecos.uiserv.app.application.api.records;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordConstants;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.SortBy;
import ru.citeck.ecos.records2.rest.RemoteRecordsUtils;
import ru.citeck.ecos.records2.source.dao.local.meta.MetaAttributesSupplier;
import ru.citeck.ecos.records2.source.dao.local.meta.MetaRecordsDaoAttsProvider;
import ru.citeck.ecos.uiserv.domain.i18n.service.I18nService;
import ru.citeck.ecos.uiserv.domain.icon.service.IconService;
import ru.citeck.ecos.uiserv.domain.journal.service.JournalService;
import ru.citeck.ecos.uiserv.domain.menu.service.MenuService;
import ru.citeck.ecos.uiserv.domain.theme.service.ThemeService;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
@RequiredArgsConstructor
public class MetaRecordAttsProvider implements MetaAttributesSupplier {

    private static final String ATT_MENU_CACHE_KEY = "menu-cache-key";
    private static final String ATT_I18N_CACHE_KEY = "i18n-cache-key";
    private static final String ATT_THEME_CACHE_KEY = "theme-cache-key";
    private static final String ATT_IMAGES_CACHE_KEY = "images-cache-key";

    private static final long TYPES_CHECK_INTERVAL = 5000;

    private final JournalService journalService;
    private final MenuService menuService;
    private final MetaRecordsDaoAttsProvider provider;
    private final RecordsService recordsService;
    private final I18nService i18nService;
    private final ThemeService themeService;
    private final IconService iconService;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private long lastTypesChecked = 0;
    private volatile Future<?> typesCheckFuture = null;

    private String lastModifiedType = "";

    @PostConstruct
    void init() {
        provider.register(this);
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

    private void updateTypesChangedTime() {

        RecordsQuery query = new RecordsQuery();
        query.setSourceId("emodel/type");
        query.addSort(new SortBy(RecordConstants.ATT_MODIFIED, false));
        query.setMaxItems(1);
        query.setLanguage(PredicateService.LANGUAGE_PREDICATE);

        RemoteRecordsUtils.runAsSystem(() -> {
            recordsService.queryRecord(query, Collections.singletonList(RecordConstants.ATT_MODIFIED))
                .ifPresent(meta -> {
                    String modified = meta.get(RecordConstants.ATT_MODIFIED).asText();
                    if (StringUtils.isNotBlank(modified)) {
                        lastModifiedType = modified;
                    }
                });
            return null;
        });
    }

    private void syncTypesChangedTime() {

        long currentTime = System.currentTimeMillis();
        if (typesCheckFuture == null && ((currentTime - lastTypesChecked) > TYPES_CHECK_INTERVAL)) {
            synchronized (this) {
                if (typesCheckFuture == null) {
                    lastTypesChecked = currentTime;
                    typesCheckFuture = executorService.submit(() -> {
                        updateTypesChangedTime();
                        typesCheckFuture = null;
                    });
                }
            }
        }
    }

    @Override
    public Object getAttribute(String attribute, MetaField field) {

        switch (attribute) {

            case ATT_MENU_CACHE_KEY:

                syncTypesChangedTime();

                return Objects.hash(
                    journalService.getLastModifiedTimeMs(),
                    menuService.getLastModifiedTimeMs(),
                    i18nService.getCacheKey(),
                    lastModifiedType
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
