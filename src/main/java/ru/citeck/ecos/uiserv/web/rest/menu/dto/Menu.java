package ru.citeck.ecos.uiserv.web.rest.menu.dto;

import java.util.List;

public class Menu {

    private String id;
    private String type;
    private List<Element> items;

    public void setId(String id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setItems(List<Element> elements) {
        this.items = elements;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public List<Element> getItems() {
        return items;
    }
}
