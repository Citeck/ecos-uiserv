package ru.citeck.ecos.uiserv.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import ru.citeck.ecos.records2.objdata.ObjectData;

/**
 * @author Roman Makarskiy
 */
@Data
public class DashboardDto implements EntityDto {

    private String id;
    private String key;
    private String type;
    private String authority;
    private ObjectData config;

    public DashboardDto() {
    }

    public DashboardDto(DashboardDto other) {
        this.id = other.id;
        this.key = other.key;
        this.type = other.type;
        this.authority = other.authority;
        this.config = other.config;
    }

    @JsonIgnore
    @Deprecated
    public void setUser(String user) {
        authority = user;
    }

    @Override
    @JsonIgnore
    @Deprecated
    public String getUser() {
        return authority;
    }
}
