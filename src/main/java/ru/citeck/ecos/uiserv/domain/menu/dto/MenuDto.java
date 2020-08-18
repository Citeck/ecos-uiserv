package ru.citeck.ecos.uiserv.domain.menu.dto;

import ecos.com.fasterxml.jackson210.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.citeck.ecos.commons.data.DataValue;

import java.util.List;
import java.util.Map;

@Data
@ToString(exclude = {"subMenu"})
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MenuDto {

    private String id;
    private String type;
    private List<String> authorities;
    private Integer version;

    private Map<String, SubMenuDto> subMenu;

    public MenuDto(String id) {
        this.id = id;
    }

    public MenuDto(MenuDto other) {
        this.id = other.id;
        this.type = other.type;
        this.version = other.version;
        this.authorities = DataValue.create(other.authorities).toStrList();
        this.subMenu = DataValue.create(other.subMenu).asMap(String.class, SubMenuDto.class);
    }
}

