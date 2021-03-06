package ru.citeck.ecos.uiserv.domain.menu.service.resolving.resolvers;

import ru.citeck.ecos.uiserv.domain.menu.service.resolving.ResolvedMenuItemDto;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface MenuItemsResolver {
    List<ResolvedMenuItemDto> resolve(Map<String, String> params, ResolvedMenuItemDto context,
                                      Function<String, String> i18n);
    String getId();
}
