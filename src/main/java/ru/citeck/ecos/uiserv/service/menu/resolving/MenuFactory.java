package ru.citeck.ecos.uiserv.service.menu.resolving;

import org.apache.commons.lang.StringUtils;
import ru.citeck.ecos.uiserv.service.menu.resolving.resolvers.MenuItemsResolver;
import ru.citeck.ecos.uiserv.service.menu.format.xml.xml.Action;
import ru.citeck.ecos.uiserv.service.menu.format.xml.xml.Evaluator;
import ru.citeck.ecos.uiserv.service.menu.format.xml.xml.Item;
import ru.citeck.ecos.uiserv.service.menu.format.xml.xml.Items;
import ru.citeck.ecos.uiserv.service.menu.format.xml.xml.ItemsResolver;
import ru.citeck.ecos.uiserv.service.menu.format.xml.xml.MenuConfig;
import ru.citeck.ecos.uiserv.service.menu.format.xml.xml.Parameter;

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

    public ResolvedMenuDto getResolvedMenu(MenuConfig menuConfigContentData) {
        ResolvedMenuDto menu = new ResolvedMenuDto();
        menu.setId(menuConfigContentData.getId());
        menu.setType(menuConfigContentData.getType());

        List<ResolvedMenuItemDto> elements = constructItems(menuConfigContentData.getItems(), null);
        menu.setItems(elements);
        return menu;
    }

    private List<ResolvedMenuItemDto> constructItems(Items items, ResolvedMenuItemDto context) {
        if (items == null) {
            return Collections.emptyList();
        }
        List<ResolvedMenuItemDto> result = new ArrayList<>();
        items.getItemsChildren()
                .forEach(obj -> {
                    if (obj instanceof Item && evaluate((Item) obj)) {
                        ResolvedMenuItemDto newElement = new ResolvedMenuItemDto();
                        if (context == null) {
                            Map<String, String> params = new HashMap<>();
                            params.put("rootElement", "true");
                            newElement.setParams(params);
                        }
                        result.add(updateItem(newElement, (Item) obj));
                    } else if (obj instanceof ItemsResolver) {
                        result.addAll(resolve((ItemsResolver) obj, context));
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
    private boolean evaluate(Item item) {
        Evaluator evaluator = item.getEvaluator();
        if (evaluator == null) {
            return true;
        }

        if (!evaluator.getId().equals(userInGroupEvaluatorId))
            //todo log WARN or ERROR
            return false;

        return evaluator.getParam().stream()
            .filter(p -> p.getName().equals("groupName"))
            .map(Parameter::getValue)
            .findAny()
            .map(authorities::contains).orElse(false);
    }

    private List<ResolvedMenuItemDto> resolve(ItemsResolver child, ResolvedMenuItemDto context) {
        String resolverId = child.getId();
        if (StringUtils.isEmpty(resolverId)) {
            return Collections.emptyList();
        }
        MenuItemsResolver resolver = resolvers.get(resolverId);
        if (resolver == null) {
            return Collections.emptyList();
        }
        Map<String, String> params = new HashMap<>();
        for (Parameter param : child.getParam()) {
            params.put(param.getName(), param.getValue());
        }

        return resolver.resolve(params, context, i18n).stream()
            .map(element -> updateItem(element, child.getItem()))
            .collect(Collectors.toList());
    }

    private ResolvedMenuItemDto updateItem(ResolvedMenuItemDto targetElement, Item newData) {
        if (targetElement == null) {
            return null;
        }
        if (newData == null) {
            return targetElement;
        }

        String label = getLocalizedMessage(newData.getLabel());
        String id = newData.getId();
        String icon = newData.getIcon();
        List<Parameter> param = newData.getParam();

        Boolean mobileVisible = newData.isMobileVisible();

        Action xmlAction = newData.getAction();
        if (xmlAction != null) {
            Map<String, String> params = new HashMap<>();
            for (Parameter xmlParam : xmlAction.getParam()) {
                params.put(xmlParam.getName(), xmlParam.getValue());
            }
            targetElement.setAction(xmlAction.getType(), params);
        }

        if (!StringUtils.isEmpty(id)) {
            targetElement.setId(id);
        }
        if (!StringUtils.isEmpty(label)) {
            targetElement.setLabel(label);
        }
        if (!StringUtils.isEmpty(icon)) {
            targetElement.setIcon(icon);
        }
        if (mobileVisible != null) {
            targetElement.setMobileVisible(mobileVisible);
        }

        if (param != null) {
            Map<String, String> params = targetElement.getParams();
            if (params == null) {
                params = new HashMap<>();
            }
            Map<String, String> newParams = new HashMap<>();
            param.forEach(parameter -> newParams.put(parameter.getName(), parameter.getValue()));
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
