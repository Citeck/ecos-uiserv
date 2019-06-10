package ru.citeck.ecos.uiserv.web.rest.menu.resolvers;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import ru.citeck.ecos.uiserv.web.rest.menu.dto.Element;

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
    public List<Element> resolve(Map<String, String> params, Element context,
                                 Function<String, String> i18n) {
        String siteId = context.getParams().get(SITE_ID_KEY);
        List<Element> result = new ArrayList<>();
        result.add(siteElement(siteId, context, i18n));
        return result;
    }

    protected Element siteElement(String siteId, Element  context,
                                  Function<String, String> i18n) {
        String parentElemId = StringUtils.defaultString(context.getId());
        String id = String.format("%s_%s", parentElemId, this.getId());
        String pageId = String.format(pageLinkTemplate, siteId);
        Map<String, String> actionParams = new HashMap<>();
        actionParams.put(PAGE_ID_KEY, pageId);
        Element element = new Element();
        element.setId(id);
        element.setLabel(i18n.apply(elementTitleKey));
        element.setAction(PAGE_LINK_KEY, actionParams);
        return element;
    }
}
