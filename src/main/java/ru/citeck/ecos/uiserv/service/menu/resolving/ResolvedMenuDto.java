package ru.citeck.ecos.uiserv.service.menu.resolving;

import java.util.List;

public class ResolvedMenuDto {

    private String id;
    private String type;
    private List<ResolvedMenuItemDto> items;

    public void setId(String id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setItems(List<ResolvedMenuItemDto> elements) {
        this.items = elements;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public List<ResolvedMenuItemDto> getItems() {
        return items;
    }
}
