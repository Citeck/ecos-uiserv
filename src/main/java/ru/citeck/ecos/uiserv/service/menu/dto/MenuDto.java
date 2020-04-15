package ru.citeck.ecos.uiserv.service.menu.dto;

import lombok.Data;

import java.util.List;

@Data
public class MenuDto {

    private String id;
    private String type;
    private List<String> authorities;
    private float priority;
    private List<MenuItemDto> items;
}
