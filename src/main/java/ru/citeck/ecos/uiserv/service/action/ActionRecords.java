package ru.citeck.ecos.uiserv.service.action;

import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDTO;
import ru.citeck.ecos.uiserv.service.entity.AbstractEntityRecords;

import java.util.*;

/**
 * @author Roman Makarskiy
 */
@Component
public class ActionRecords extends AbstractEntityRecords<ActionDTO> {

    public static final String ID = "action";

    private static final String MODE_CARD = "card";
    private static final String MODE_JOURNAL = "journal";

    private final ActionService actionService;

    @Autowired
    public ActionRecords(ActionService actionService, ActionEntityService actionEntityService) {
        setId(ID);
        this.entityService = actionEntityService;
        this.actionService = actionService;
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
    public RecordsQueryResult<ActionDTO> getMetaValues(RecordsQuery recordsQuery) {
        ActionQuery query = recordsQuery.getQuery(ActionQuery.class);

        RecordRef record = query.getRecord();
        if (record == null) {
            throw new IllegalArgumentException("You must specify a record for querying");
        }

        String mode = query.getMode();
        if (StringUtils.isBlank(mode)) {
            throw new IllegalArgumentException("You must specify a mode for querying");
        }

        List<ActionDTO> actions;

        switch (mode) {
            case MODE_JOURNAL:
                actions = actionService.getJournalActions(record, query.getMode(), query.getScope());
                break;
            case MODE_CARD:
                actions = actionService.getCardActions();
                break;
            default:
                throw new IllegalArgumentException(String.format("Mode: <%s> not supported", mode));
        }

        RecordsQueryResult<ActionDTO> result = new RecordsQueryResult<>();
        result.setRecords(actions);
        result.setTotalCount(actions.size());

        return result;
    }

    @Override
    protected ActionDTO getEmpty() {
        return new ActionDTO();
    }

    @Data
    private static class ActionQuery {
        private RecordRef record;
        private String mode;
        private String scope;
    }
}
