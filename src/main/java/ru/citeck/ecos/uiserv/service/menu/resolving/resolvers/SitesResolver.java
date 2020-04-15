package ru.citeck.ecos.uiserv.service.menu.resolving.resolvers;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.request.query.QueryConsistency;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.uiserv.service.menu.resolving.ResolvedMenuItemDto;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class SitesResolver {
    protected static final String SITE_NAME_KEY = "siteName";
    private static final String SITE_ID_KEY = "siteId";
    private static final String LIST_ID_KEY = "listId";
    private static final String ROOT_ELEMENT_KEY = "rootElement";

    private final RecordsService recordsService;
    private final String actionKey;
    private final String defaultListId;

    public List<ResolvedMenuItemDto> resolve(Map<String, String> params, ResolvedMenuItemDto context) {
        return getUserSites().stream()
            .map(siteInfo -> constructItem(siteInfo, context,
                context.getParams().containsKey(ROOT_ELEMENT_KEY),
                Optional.ofNullable(context.getParams().get(LIST_ID_KEY)).orElse(defaultListId)))
            .collect(Collectors.toList());
    }

    protected ResolvedMenuItemDto constructItem(SiteInfo attrs, ResolvedMenuItemDto context, boolean displayIcon, String listId) {
        String siteName = attrs.getName();
        String parentElemId = StringUtils.defaultString(context.getId());
        ResolvedMenuItemDto element = new ResolvedMenuItemDto();
        Map<String, String> actionParams = new HashMap<>();
        actionParams.put(SITE_NAME_KEY, siteName);
        if (listId != null) {
            actionParams.put(LIST_ID_KEY, listId);
        }
        element.setLabel(attrs.getTitle());
        Map<String, String> elementParams = new HashMap<>();
        elementParams.put(SITE_ID_KEY, siteName);
        if (listId != null) {
            elementParams.put(LIST_ID_KEY, listId);
        }
        element.setParams(elementParams);
        element.setId(String.format("%s_%s", parentElemId, siteName.toUpperCase()));
        element.setAction(actionKey, actionParams);
        if (displayIcon) {
            element.setIcon(siteName);
        }
        return element;
    }

    protected static class SiteInfo {
        private String name;
        private String title;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    private Collection<SiteInfo> getUserSites() {
        RecordsQuery query = new RecordsQuery();
        query.setSourceId("alfresco/");
        query.setQuery("TYPE:'st:site'");
        query.setLanguage("fts-alfresco");
        query.setConsistency(QueryConsistency.TRANSACTIONAL);

        RecordsQueryResult<SiteInfo> result = recordsService.queryRecords(query, SiteInfo.class);
        return result.getRecords();
    }
}
