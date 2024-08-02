package ru.citeck.ecos.uiserv.domain.action.dto;

import lombok.Data;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class RecordsActionsDto {

    private List<ActionDto> actions;
    private Map<EntityRef, Set<String>> recordActions;

    public List<ActionDto> getActions() {
        return actions;
    }

    public void setActions(List<ActionDto> actions) {
        this.actions = actions;
    }

    public Map<EntityRef, Set<String>> getRecordActions() {
        return recordActions;
    }

    public void setRecordActions(Map<EntityRef, Set<String>> recordActions) {
        this.recordActions = recordActions;
    }
}
