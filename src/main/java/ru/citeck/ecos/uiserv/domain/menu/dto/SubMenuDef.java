package ru.citeck.ecos.uiserv.domain.menu.dto;

import lombok.Data;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@IncludeNonDefault
public class SubMenuDef {

    private ObjectData config = ObjectData.create();
    private List<MenuItemDef> items = new ArrayList<>();
    private List<String> allowedFor = new ArrayList<>();

    public List<String> getAllowedFor() {
        return allowedFor;
    }

    public void setAllowedFor(List<String> allowedFor) {
        this.allowedFor = Objects.requireNonNullElseGet(allowedFor, ArrayList::new);
    }

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


