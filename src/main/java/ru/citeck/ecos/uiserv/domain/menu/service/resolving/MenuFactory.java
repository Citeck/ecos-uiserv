package ru.citeck.ecos.uiserv.domain.menu.service.resolving;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDto;
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuItemActionDef;
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuItemDef;
import ru.citeck.ecos.uiserv.domain.menu.dto.SubMenuDef;
import ru.citeck.ecos.uiserv.domain.menu.service.resolving.resolvers.MenuItemsResolver;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MenuFactory {

    private final Function<String, String> i18n;
    private final Map<String, MenuItemsResolver> resolvers;

    public MenuFactory(Function<String, String> i18n,
                       List<MenuItemsResolver> resolvers) {

        this.i18n = i18n;
        this.resolvers = resolvers.stream().collect(Collectors.toMap(MenuItemsResolver::getId, Function.identity()));
    }

    public ResolvedMenuDto getResolvedMenu(MenuDto menuDto) {
        ResolvedMenuDto menu = new ResolvedMenuDto();

        menu.setId(menuDto.getId());
        menu.setType(menuDto.getType());
        menu.setItems(subMenusToItemDtos(menuDto.getSubMenu()));

        return menu;
    }

    private List<ResolvedMenuItemDto> subMenusToItemDtos(Map<String, SubMenuDef> subMenuDtoMap) {
        SubMenuDef left = subMenuDtoMap.get("left");
        if (left == null) {
            return Collections.emptyList();
        }

        return constructItems(left.getItems(), null);
    }

    private List<ResolvedMenuItemDto> constructItems(List<MenuItemDef> items, ResolvedMenuItemDto context) {
        if (items == null) {
            return Collections.emptyList();
        }
        List<ResolvedMenuItemDto> result = new ArrayList<>();

        items.forEach(itemDto -> {
            ResolvedMenuItemDto newElement = new ResolvedMenuItemDto();
            if (context == null) {
                Map<String, String> params = new HashMap<>();
                params.put("rootElement", "true");
                newElement.setParams(params);
            }
            result.add(updateItem(newElement, itemDto));
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
    private boolean evaluate(MenuItemDef item) {
        return true;
        /*RecordEvaluatorDto evaluator = item.getEvaluator();
        if (evaluator == null) {
            return true;
        }

        if (!evaluator.getId().equals(userInGroupEvaluatorId)) {
            //todo log WARN or ERROR
            return false;
        }

        DataValue groupNameValue = evaluator.getConfig().get("groupName");
        if (groupNameValue.isNull()) {
            return false;
        }

        String[] groupNames = groupNameValue.asText().split(",");
        for (String groupName: groupNames) {
            if (authorities.contains(groupName.trim())) {
                return true;
            }
        }

        return false; */
    }

    private List<ResolvedMenuItemDto> resolve(MenuItemDef child, ResolvedMenuItemDto context) {
        String resolverId = child.getId();
        if (StringUtils.isEmpty(resolverId)) {
            return Collections.emptyList();
        }
        MenuItemsResolver resolver = resolvers.get(resolverId);
        if (resolver == null) {
            return Collections.emptyList();
        }
        Map<String, String> params = new HashMap<>();
        child.getConfig().forEachJ((k, v) -> {
            params.put(k, v.textValue());
        });

        MenuItemDef childItem = child.getItems() != null && !child.getItems().isEmpty()
            ? child.getItems().get(0)
            : null;

        return resolver.resolve(params, context, i18n).stream()
            .map(element -> updateItem(element, childItem))
            .collect(Collectors.toList());
    }

    private ResolvedMenuItemDto updateItem(ResolvedMenuItemDto targetElement, MenuItemDef newData) {
        if (targetElement == null) {
            return null;
        }
        if (newData == null) {
            return targetElement;
        }

        MenuItemActionDef actionDto = newData.getAction();
        if (actionDto != null) {
            Map<String, String> params = new HashMap<>();
            actionDto.getConfig().forEachJ((k, v) -> {
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

        EntityRef icon = newData.getIcon();
        if (!EntityRef.isEmpty(icon)) {
            targetElement.setIcon(icon.toString());
        }

        ObjectData config = newData.getConfig();
        if (config != null) {
            Map<String, String> params = targetElement.getParams();
            if (params == null) {
                params = new HashMap<>();
            }
            Map<String, String> newParams = new HashMap<>();
            config.forEachJ((k, v) -> newParams.put(k, v.textValue()));
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
