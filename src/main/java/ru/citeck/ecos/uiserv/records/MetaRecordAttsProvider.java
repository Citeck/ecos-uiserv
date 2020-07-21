package ru.citeck.ecos.uiserv.records;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.source.dao.local.meta.MetaAttributesSupplier;
import ru.citeck.ecos.records2.source.dao.local.meta.MetaRecordsDaoAttsProvider;
import ru.citeck.ecos.uiserv.domain.journal.service.JournalService;
import ru.citeck.ecos.uiserv.domain.menu.service.MenuService;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class MetaRecordAttsProvider implements MetaAttributesSupplier {

    private static final String ATT_MENU_CACHE_KEY = "menu-cache-key";

    private final JournalService journalService;
    private final MenuService menuService;
    private final MetaRecordsDaoAttsProvider provider;

    @PostConstruct
    void init() {
        provider.register(this);
    }

    @Override
    public List<String> getAttributesList() {
        return Collections.singletonList(ATT_MENU_CACHE_KEY);
    }

    @Override
    public Object getAttribute(String attribute, MetaField field) {
        return Objects.hash(
            journalService.getLastModifiedTimeMs(),
            menuService.getLastModifiedTimeMs()
        );
    }
}
