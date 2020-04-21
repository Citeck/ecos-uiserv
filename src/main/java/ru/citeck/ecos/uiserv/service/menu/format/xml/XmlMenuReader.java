package ru.citeck.ecos.uiserv.service.menu.format.xml;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorDto;
import ru.citeck.ecos.uiserv.service.menu.dto.MenuDto;
import ru.citeck.ecos.uiserv.service.menu.dto.MenuItemActionDto;
import ru.citeck.ecos.uiserv.service.menu.dto.MenuItemDto;
import ru.citeck.ecos.uiserv.service.menu.dto.SubMenuDto;
import ru.citeck.ecos.uiserv.service.menu.format.MenuReader;
import ru.citeck.ecos.uiserv.service.menu.format.xml.xml.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static ru.citeck.ecos.uiserv.service.menu.dto.MenuItemDto.TYPE_ITEM;
import static ru.citeck.ecos.uiserv.service.menu.dto.MenuItemDto.TYPE_RESOLVER;

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

        dto.setPriority(0);
        dto.setSubMenu(parseSubMenusFromItems(config.getItems()));

        return dto;
    }

    private Map<String, SubMenuDto> parseSubMenusFromItems(Items items) {
        SubMenuDto left = new SubMenuDto();
        List<MenuItemDto> menuItemDtos = items.getItemsChildren().stream()
            .map(this::xmlItemToDto)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        left.setItems(menuItemDtos);

        Map<String, SubMenuDto> subMenus = new HashMap<>();
        subMenus.put("left", left);
        return subMenus;
    }

    private MenuItemDto xmlItemToDto(Object item) {
        if (item instanceof ItemsResolver) {
            return itemsResolverToDto((ItemsResolver) item);
        }

        if (item instanceof Item) {
            return itemToDto((Item) item);
        }

        return null;
    }

    private MenuItemDto itemsResolverToDto(ItemsResolver resolver) {
        MenuItemDto dto = new MenuItemDto();

        dto.setId(resolver.getId());
        dto.setType(TYPE_RESOLVER);
        if (resolver.getParam() != null) {
            dto.setConfig(parameterListToObjectData(resolver.getParam()));
        }
        if (resolver.getItem() != null) {
            List<MenuItemDto> singleMenuItemDto = new ArrayList<>();
            singleMenuItemDto.add(itemToDto(resolver.getItem()));
            dto.setItems(singleMenuItemDto);
        }

        return dto;
    }

    private MenuItemDto itemToDto(Item item) {
        MenuItemDto dto = new MenuItemDto();

        dto.setId(item.getId());
        dto.setType(TYPE_ITEM);
        if (item.getLabel() != null) {
            dto.setLabel(new MLText(item.getLabel()));
        }
        dto.setIcon(item.getIcon());

        Boolean isMobileVisible = item.isMobileVisible();
        dto.setMobileVisible(isMobileVisible != null ? isMobileVisible : false);
        if (item.getAction() != null) {
            dto.setAction(actionDtoFromAction(item.getAction()));
        }

        if (item.getParam() != null) {
            dto.setConfig(parameterListToObjectData(item.getParam()));
        }

        if (item.getEvaluator() != null) {
            dto.setEvaluator(evaluatorDtoFromEvaluator(item.getEvaluator()));
        }
        if (item.getItems() != null) {
            List<MenuItemDto> items = item.getItems().getItemsChildren().stream()
                .map(this::xmlItemToDto)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            dto.setItems(items);
        }

        return dto;
    }

    private MenuItemActionDto actionDtoFromAction(Action action) {
        MenuItemActionDto actionDto = new MenuItemActionDto();

        actionDto.setType(action.getType());
        if (action.getParam() != null) {
            actionDto.setConfig(parameterListToObjectData(action.getParam()));
        }

        return actionDto;
    }

    private RecordEvaluatorDto evaluatorDtoFromEvaluator(Evaluator evaluator) {
        RecordEvaluatorDto recordEvaluatorDto = new RecordEvaluatorDto();

        recordEvaluatorDto.setId(evaluator.getId());
        if (evaluator.getParam() != null) {
            recordEvaluatorDto.setConfig(parameterListToObjectData(evaluator.getParam()));
        }

        return recordEvaluatorDto;
    }

    private ObjectData parameterListToObjectData(List<Parameter> parameters) {
        Map<String, String> parametersMap = parameters
            .stream()
            .collect(Collectors.toMap(Parameter::getName, Parameter::getValue));
        return new ObjectData(parametersMap);
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
