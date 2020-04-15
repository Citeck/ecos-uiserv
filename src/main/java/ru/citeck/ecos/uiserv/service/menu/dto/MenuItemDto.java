package ru.citeck.ecos.uiserv.service.menu.dto;

import lombok.Data;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorDto;

import java.util.List;

@Data
public class MenuItemDto {

    private String id;

    // "item" or "resolver"
    private String type;
    private MLText label;
    private String icon;

    private boolean mobileVisible;
    private MenuItemActionDto action;

    private ObjectData config;
    private RecordEvaluatorDto evaluator;

    private List<MenuItemDto> items;
}
