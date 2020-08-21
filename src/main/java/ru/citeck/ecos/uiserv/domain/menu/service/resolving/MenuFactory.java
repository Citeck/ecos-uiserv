package ru.citeck.ecos.uiserv.domain.menu.service.resolving;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorDto;
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDto;
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuItemActionDto;
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuItemDto;
import ru.citeck.ecos.uiserv.domain.menu.dto.SubMenuDto;
import ru.citeck.ecos.uiserv.domain.menu.service.resolving.resolvers.MenuItemsResolver;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MenuFactory {

    private final static String userInGroupEvaluatorId = "user-in-group";

    private final Set<String> authorities;
    private final Function<String, String> i18n;
    private final Map<String, MenuItemsResolver> resolvers;

    public MenuFactory(Set<String> userAuthorities,
                       Function<String, String> i18n,
                       List<MenuItemsResolver> resolvers) {

        this.i18n = i18n;
        this.authorities = userAuthorities;
        this.resolvers = resolvers.stream().collect(Collectors.toMap(MenuItemsResolver::getId, Function.identity()));
    }

    public ResolvedMenuDto getResolvedMenu(MenuDto menuDto) {
        ResolvedMenuDto menu = new ResolvedMenuDto();

        menu.setId(menuDto.getId());
        menu.setType(menuDto.getType());
        menu.setItems(subMenusToItemDtos(menuDto.getSubMenu()));

        return menu;
    }

    private List<ResolvedMenuItemDto> subMenusToItemDtos(Map<String, SubMenuDto> subMenuDtoMap) {
        SubMenuDto left = subMenuDtoMap.get("left");
        if (left == null) {
            return Collections.emptyList();
        }

        return constructItems(left.getItems(), null);
    }

    private List<ResolvedMenuItemDto> constructItems(List<MenuItemDto> items, ResolvedMenuItemDto context) {
        if (items == null) {
            return Collections.emptyList();
        }
        List<ResolvedMenuItemDto> result = new ArrayList<>();

        items.forEach(itemDto -> {
            if (itemDto.isItem() && evaluate(itemDto)) {
                ResolvedMenuItemDto newElement = new ResolvedMenuItemDto();
                if (context == null) {
                    Map<String, String> params = new HashMap<>();
                    params.put("rootElement", "true");
                    newElement.setParams(params);
                }
                result.add(updateItem(newElement, itemDto));
            } else if (itemDto.isResolver()) {
                result.addAll(resolve(itemDto, context));
            }
        });
        return filterElements(result, context);
    }

    private List<ResolvedMenuItemDto> filterElements(List<ResolvedMenuItemDto> elements, ResolvedMenuItemDto context) {
        if (context == null || context.getParams() == null) {
            return elements;
        }
        String hideParam = StringUtils.defaultString(context.getParams().get("hideEmpty"));
        if (!hideParam.equals("true")) {
            return elements;
        }
        Predicate<ResolvedMenuItemDto> predicate = elem -> {
            boolean ignore = false;
            if (elem.getParams() != null) {
                String ignoreParam = StringUtils.defaultString(elem.getParams().get("ignoreHideEmpty"));
                ignore = ignoreParam.equals("true");
            }
            if (ignore) {
                return true;
            }
            return !Optional.ofNullable(elem.getItems()).orElse(Collections.emptyList()).isEmpty();
        };
        return elements.stream().filter(predicate).collect(Collectors.toList());
    }

    //todo Since we decided to only allow privileges check as opposed to other "evaluators"
    //(some of them can be slow because they ask something of ecos),
    // we'd better change XML schema accordingly, to avoid having ugly code like this:
    private boolean evaluate(MenuItemDto item) {
        RecordEvaluatorDto evaluator = item.getEvaluator();
        if (evaluator == null) {
            return true;
        }

        if (!evaluator.getId().equals(userInGroupEvaluatorId)) {
            //todo log WARN or ERROR
            return false;
        }

        DataValue groupName = evaluator.getConfig().get("groupName");
        if (groupName.isNull()) {
            return false;
        }
        return authorities.contains(groupName.asText());
    }

    private List<ResolvedMenuItemDto> resolve(MenuItemDto child, ResolvedMenuItemDto context) {
        String resolverId = child.getId();
        if (StringUtils.isEmpty(resolverId)) {
            return Collections.emptyList();
        }
        MenuItemsResolver resolver = resolvers.get(resolverId);
        if (resolver == null) {
            return Collections.emptyList();
        }
        Map<String, String> params = new HashMap<>();
        child.getConfig().forEach((k, v) -> {
            params.put(k, v.textValue());
        });

        MenuItemDto childItem = child.getItems() != null && !child.getItems().isEmpty()
            ? child.getItems().get(0)
            : null;

        return resolver.resolve(params, context, i18n).stream()
            .map(element -> updateItem(element, childItem))
            .collect(Collectors.toList());
    }

    private ResolvedMenuItemDto updateItem(ResolvedMenuItemDto targetElement, MenuItemDto newData) {
        if (targetElement == null) {
            return null;
        }
        if (newData == null) {
            return targetElement;
        }

        MenuItemActionDto actionDto = newData.getAction();
        if (actionDto != null) {
            Map<String, String> params = new HashMap<>();
            actionDto.getConfig().forEach((k, v) -> {
                params.put(k, v.textValue());
            });
            targetElement.setAction(actionDto.getType(), params);
        }

        String id = newData.getId();
        if (!StringUtils.isEmpty(id)) {
            targetElement.setId(id);
        }

        MLText mlLabel = newData.getLabel();
        String label = null;
        if (mlLabel != null) {
            Locale locale = LocaleContextHolder.getLocale();
            label = getLocalizedMessage(mlLabel.getClosestValue(locale));
        }
        if (!StringUtils.isEmpty(label)) {
            targetElement.setLabel(label);
        }

        String icon = newData.getIcon();
        if (!StringUtils.isEmpty(icon)) {
            targetElement.setIcon(icon);
        }

        ObjectData config = newData.getConfig();
        if (config != null) {
            Map<String, String> params = targetElement.getParams();
            if (params == null) {
                params = new HashMap<>();
            }
            Map<String, String> newParams = new HashMap<>();
            config.forEach((k, v) -> newParams.put(k, v.textValue()));
            params.putAll(newParams);
            targetElement.setParams(params);
        }

        targetElement.setItems(constructItems(newData.getItems(), targetElement));

        return targetElement;
    }

    private String getLocalizedMessage(String messageKey) {
        if (StringUtils.isEmpty(messageKey)) {
            return StringUtils.EMPTY;
        }
        return i18n.apply(messageKey);
    }
}