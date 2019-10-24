package ru.citeck.ecos.uiserv.domain.action.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.citeck.ecos.uiserv.domain.EntityDTO;

/**
 * @author Roman Makarskiy
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ActionDTO extends ru.citeck.ecos.apps.app.module.type.action.ActionDTO implements EntityDTO {

    @Override
    public String getUser() {
        return null;
    }
}
