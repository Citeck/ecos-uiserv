package ru.citeck.ecos.uiserv.web.rest.menu.resolvers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.uiserv.web.rest.menu.dto.Element;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Site resolver with site references link instead of site link
 */
@Component
public class SitesToReferenceDataResolver implements MenuItemsResolver {
    private static final String JOURNAL_LINK_KEY = "JOURNAL_LINK";
    private static final String DEFAULT_LIST_ID = "references";

    private final SitesResolver sitesResolver;

    @Autowired
    public SitesToReferenceDataResolver(RecordsService recordsService) {
        sitesResolver = new SitesResolver(recordsService, JOURNAL_LINK_KEY, DEFAULT_LIST_ID);
    }

    @Override
    public List<Element> resolve(Map<String, String> params, Element context,
                                 Function<String, String> i18n) {
        return sitesResolver.resolve(params, context);
    }

    @Override
    public String getId() {
        return "USER_SITES_REFERENCES";
    }
}

