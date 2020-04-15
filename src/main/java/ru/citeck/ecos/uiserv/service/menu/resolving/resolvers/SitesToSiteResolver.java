package ru.citeck.ecos.uiserv.service.menu.resolving.resolvers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.uiserv.service.menu.resolving.ResolvedMenuItemDto;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class SitesToSiteResolver implements MenuItemsResolver {
    private static final String SITE_LINK_KEY = "SITE_LINK";

    private final SitesResolver sitesResolver;

    @Autowired
    SitesToSiteResolver(RecordsService recordsService) {
        sitesResolver = new SitesResolver(recordsService, SITE_LINK_KEY, null);
    }

    @Override
    public List<ResolvedMenuItemDto> resolve(Map<String, String> params, ResolvedMenuItemDto context,
                                             Function<String, String> i18n) {
        return sitesResolver.resolve(params, context);
    }

    @Override
    public String getId() {
        return "USER_SITES";
    }
}
