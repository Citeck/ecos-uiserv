package ru.citeck.ecos.uiserv.domain.menu.service.format.xml;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDto;
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuItemActionDef;
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuItemDef;
import ru.citeck.ecos.uiserv.domain.menu.dto.SubMenuDef;
import ru.citeck.ecos.uiserv.domain.menu.service.format.MenuReader;
import ru.citeck.ecos.uiserv.domain.menu.service.format.xml.xml.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class XmlMenuReader implements MenuReader {

    private final JAXBContext jaxbContext;

    public XmlMenuReader() {
        try {
            jaxbContext = JAXBContext.newInstance(MenuConfig.class);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public MenuDto readData(byte[] data) {
        MenuConfig config = unmarshal(data);

        return configToDto(config);
    }

    private MenuDto configToDto(MenuConfig config) {
        MenuDto dto = new MenuDto();

        dto.setId(config.getId());
        dto.setType(config.getType());

        List<String> authorities = Arrays.stream(config.getAuthorities().split(","))
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
        dto.setAuthorities(authorities);

        dto.setVersion(null);
        dto.setSubMenu(parseSubMenusFromItems(config.getItems()));

        return dto;
    }

    private Map<String, SubMenuDef> parseSubMenusFromItems(Items items) {
        SubMenuDef left = new SubMenuDef();
        List<MenuItemDef> menuItemDefs = items.getItemsChildren().stream()
            .map(this::xmlItemToDto)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        left.setItems(menuItemDefs);

        Map<String, SubMenuDef> subMenus = new HashMap<>();
        subMenus.put("left", left);
        return subMenus;
    }

    private MenuItemDef xmlItemToDto(Object item) {
        if (item instanceof ItemsResolver) {
            return itemsResolverToDto((ItemsResolver) item);
        }

        if (item instanceof Item) {
            return itemToDto((Item) item);
        }

        return null;
    }

    private MenuItemDef itemsResolverToDto(ItemsResolver resolver) {
        MenuItemDef.Builder dto = MenuItemDef.create();

        dto.withId(resolver.getId());
        //dto.withType(TYPE_RESOLVER);
        if (resolver.getParam() != null) {
            dto.setConfig(parameterListToObjectData(resolver.getParam()));
        }
        if (resolver.getItem() != null) {
            List<MenuItemDef> singleMenuItemDef = new ArrayList<>();
            singleMenuItemDef.add(itemToDto(resolver.getItem()));
            dto.setItems(singleMenuItemDef);
        }

        return dto.build();
    }

    private MenuItemDef itemToDto(Item item) {
        MenuItemDef.Builder dto = MenuItemDef.create();

        dto.withId(item.getId());
        dto.withType("item");
        if (item.getLabel() != null) {
            dto.withLabel(new MLText(item.getLabel()));
        }
        dto.withIcon(RecordRef.valueOf(item.getIcon()));

        if (item.getAction() != null) {
            dto.withAction(actionDtoFromAction(item.getAction()));
        }

        if (item.getParam() != null) {
            dto.withConfig(parameterListToObjectData(item.getParam()));
        }

        if (item.getItems() != null) {
            List<MenuItemDef> items = item.getItems().getItemsChildren().stream()
                .map(this::xmlItemToDto)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            dto.withItems(items);
        }

        return dto.build();
    }

    private MenuItemActionDef actionDtoFromAction(Action action) {
        MenuItemActionDef actionDto = new MenuItemActionDef(
            action.getType(),
            parameterListToObjectData(action.getParam())
        );
        return actionDto;
    }

    private ObjectData parameterListToObjectData(List<Parameter> parameters) {
        Map<String, String> parametersMap = parameters
            .stream()
            .collect(Collectors.toMap(Parameter::getName, Parameter::getValue));
        return ObjectData.create(parametersMap);
    }

    private MenuConfig unmarshal(byte[] xml) {
        try {
            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            try (final InputStream input = new ByteArrayInputStream(xml)) {
                return (MenuConfig) unmarshaller.unmarshal(input);
            }
        } catch (JAXBException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getExtensions() {
        return Collections.singletonList("xml");
    }
}
