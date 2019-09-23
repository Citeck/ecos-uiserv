package ru.citeck.ecos.uiserv.service.action;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.domain.ConfigDTO;
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDTO;
import ru.citeck.ecos.uiserv.service.entity.AbstractEntityRecords;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Roman Makarskiy
 */
@Component
public class ActionRecords extends AbstractEntityRecords<ActionDTO> {

    public static final String ID = "action";

    @Autowired
    public ActionRecords(ActionService actionService) {
        setId(ID);
        this.entityService = actionService;
    }

    @Override
    public List<ActionDTO> getValuesToMutate(List<RecordRef> records) {
        List<ActionDTO> result = new ArrayList<>();
        for (RecordRef recordRef : records) {
            String id = recordRef.getId();
            if (StringUtils.isBlank(id)) {
                result.add(getEmpty());
                continue;
            }

            Optional<ActionDTO> found = entityService.getById(id);
            if (found.isPresent()) {
                result.add(found.get());
            } else {
                ActionDTO dto = new ActionDTO();
                dto.setId(id);
                result.add(dto);
            }
        }
        return result;
    }

    @Override
    protected ActionDTO getEmpty() {
        return new ActionDTO();
    }
}
