package ru.citeck.ecos.uiserv.domain.menu.service.resolving.resolvers;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import ru.citeck.ecos.uiserv.domain.menu.service.resolving.ResolvedMenuItemDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class SiteElementResolver implements MenuItemsResolver {
    private static final String SITE_ID_KEY = "siteId";
    private static final String PAGE_LINK_KEY = "PAGE_LINK";
    private static final String PAGE_ID_KEY = "pageId";

    @Getter
    @Setter
    private String id;

    @Setter
    private String pageLinkTemplate;

    @Setter
    private String elementTitleKey;

    @Override
    public List<ResolvedMenuItemDto> resolve(Map<String, String> params, ResolvedMenuItemDto context,
                                             Function<String, String> i18n) {
        String siteId = context.getParams().get(SITE_ID_KEY);
        List<ResolvedMenuItemDto> result = new ArrayList<>();
        result.add(siteElement(siteId, context, i18n));
        return result;
    }

    protected ResolvedMenuItemDto siteElement(String siteId, ResolvedMenuItemDto context,
                                              Function<String, String> i18n) {
        String parentElemId = StringUtils.defaultString(context.getId());
        String id = String.format("%s_%s", parentElemId, this.getId());
        String pageId = String.format(pageLinkTemplate, siteId);
        Map<String, String> actionParams = new HashMap<>();
        actionParams.put(PAGE_ID_KEY, pageId);
        ResolvedMenuItemDto element = new ResolvedMenuItemDto();
        element.setId(id);
        element.setLabel(i18n.apply(elementTitleKey));
        element.setAction(PAGE_LINK_KEY, actionParams);
        return element;
    }
}
