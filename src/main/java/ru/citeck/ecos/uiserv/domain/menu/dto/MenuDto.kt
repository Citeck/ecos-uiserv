package ru.citeck.ecos.uiserv.domain.menu.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault;

import java.util.List;
import java.util.Map;

@Data
@ToString(exclude = {"subMenu"})
@NoArgsConstructor
@IncludeNonDefault
public class MenuDto {

    private String id;
    private String type;
    private List<String> authorities;
    private Integer version;

    private Map<String, SubMenuDef> subMenu;

    public MenuDto(String id) {
        this.id = id;
    }

    public MenuDto(MenuDto other) {
        this.id = other.id;
        this.type = other.type;
        this.version = other.version;
        this.authorities = DataValue.create(other.authorities).toStrList();
        this.subMenu = DataValue.create(other.subMenu).asMap(String.class, SubMenuDef.class);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(List<String> authorities) {
        this.authorities = authorities;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Map<String, SubMenuDef> getSubMenu() {
        return subMenu;
    }

    public void setSubMenu(Map<String, SubMenuDef> subMenu) {
        this.subMenu = subMenu;
    }
}

