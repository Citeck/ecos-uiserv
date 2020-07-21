package ru.citeck.ecos.uiserv.domain.menu.dto;

import ecos.com.fasterxml.jackson210.annotation.JsonIgnore;
import ecos.com.fasterxml.jackson210.annotation.JsonInclude;
import lombok.Data;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorDto;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MenuItemDto {
    public static final String TYPE_ITEM = "item";
    public static final String TYPE_RESOLVER = "resolver";

    private String id;

    private MLText label;
    private String icon;
    private Boolean hidden;

    private String type;
    private ObjectData config;

    private RecordEvaluatorDto evaluator;

    private MenuItemActionDto action;
    private List<MenuItemDto> items;

    @JsonIgnore
    public boolean isItem() {
        return TYPE_ITEM.equals(this.type);
    }

    @JsonIgnore
    public boolean isResolver() {
        return TYPE_RESOLVER.equals(this.type);
    }
}
