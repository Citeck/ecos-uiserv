package ru.citeck.ecos.uiserv.service.action;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDTO;

import java.util.Collections;
import java.util.List;

@Service
public class ActionService {

    public List<ActionDTO> getCardActions() {
        //TODO: implement
        return Collections.emptyList();
    }

    public List<ActionDTO> getJournalActions(RecordRef record, String mode, String scope) {
        if (StringUtils.isBlank(scope)) {
            throw new IllegalArgumentException("You must specify scope, for journal mode");
        }

        return Collections.emptyList();
    }


}
