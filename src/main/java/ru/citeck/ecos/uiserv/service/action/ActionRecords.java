package ru.citeck.ecos.uiserv.service.action;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.CrudRecordsDAO;
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDTO;

import java.util.List;

/**
 * @author Roman Makarskiy
 */
@Component
public class ActionRecords extends CrudRecordsDAO<ActionDTO> {

    public static final String ID = "action";

    private final ActionService actionService;

    @Autowired
    public ActionRecords(ActionService actionService) {
        setId(ID);
        this.actionService = actionService;
    }

    @Override
    public List<ActionDTO> getValuesToMutate(List<RecordRef> list) {
        return null;
    }

    @Override
    public RecordsMutResult save(List<ActionDTO> list) {
        return null;
    }

    @Override
    public RecordsDelResult delete(RecordsDeletion recordsDeletion) {
        return null;
    }

    @Override
    public List<ActionDTO> getMetaValues(List<RecordRef> list) {
        return null;
    }

    @Override
    public RecordsQueryResult<ActionDTO> getMetaValues(RecordsQuery recordsQuery) {
        return null;
    }
}
