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
public class MenuItemDef {

    public static final String TYPE_ITEM = "item";
    public static final String TYPE_RESOLVER = "resolver";

    private String id;

    private MLText label;
    private String icon;
    private Boolean hidden;

    private String type;
    private ObjectData config;

    private RecordEvaluatorDto evaluator;

    private MenuItemActionDef action;
    private List<MenuItemDef> items;

    @JsonIgnore
    public boolean isItem() {
        return TYPE_ITEM.equals(this.type);
    }

    @JsonIgnore
    public boolean isResolver() {
        return TYPE_RESOLVER.equals(this.type);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public MLText getLabel() {
        return label;
    }

    public void setLabel(MLText label) {
        this.label = label;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Boolean getHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ObjectData getConfig() {
        return config;
    }

    public void setConfig(ObjectData config) {
        this.config = config;
    }

    public RecordEvaluatorDto getEvaluator() {
        return evaluator;
    }

    public void setEvaluator(RecordEvaluatorDto evaluator) {
        this.evaluator = evaluator;
    }

    public MenuItemActionDef getAction() {
        return action;
    }

    public void setAction(MenuItemActionDef action) {
        this.action = action;
    }

    public List<MenuItemDef> getItems() {
        return items;
    }

    public void setItems(List<MenuItemDef> items) {
        this.items = items;
    }
}
