package ru.citeck.ecos.uiserv.service.action;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDTO;
import ru.citeck.ecos.uiserv.service.entity.AbstractEntityRecords;

/**
 * @author Roman Makarskiy
 */
@Component
public class ActionRecords extends AbstractEntityRecords<ActionDTO> {

    public static final String ID = "action";
;
    @Autowired
    public ActionRecords(ActionService actionService) {
        setId(ID);
        this.entityService = actionService;
    }

    @Override
    protected ActionDTO getEmpty() {
        return new ActionDTO();
    }
}
