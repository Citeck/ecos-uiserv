package ru.citeck.ecos.uiserv.domain.menu.dto;

import ecos.com.fasterxml.jackson210.annotation.JsonInclude;
import lombok.Data;
import ru.citeck.ecos.commons.data.ObjectData;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SubMenuDef {

    private ObjectData config = ObjectData.create();
    private List<MenuItemDef> items = new ArrayList<>();

    public ObjectData getConfig() {
        return config;
    }

    public void setConfig(ObjectData config) {
        this.config = config;
    }

    public List<MenuItemDef> getItems() {
        return items;
    }

    public void setItems(List<MenuItemDef> items) {
        this.items = items;
    }
}


