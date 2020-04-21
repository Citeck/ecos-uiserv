package ru.citeck.ecos.uiserv.service.menu.dto;

import lombok.Data;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Data
@ToString(exclude = {"subMenu"})
public class MenuDto {

    private String id;
    private String type;
    private List<String> authorities;
    private float priority;

    private Map<String, SubMenuDto> subMenu;
}

