package ru.citeck.ecos.uiserv.web.rest.menu.resolvers;

import ru.citeck.ecos.uiserv.web.rest.menu.dto.Element;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface MenuItemsResolver {
    List<Element> resolve(Map<String, String> params, Element context,
                          Function<String, String> i18n);
    String getId();
}
