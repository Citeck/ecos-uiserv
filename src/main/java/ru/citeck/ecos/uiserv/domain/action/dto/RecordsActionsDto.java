package ru.citeck.ecos.uiserv.domain.action.dto;

import lombok.Data;
import ru.citeck.ecos.records2.RecordRef;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class RecordsActionsDto {
    private List<ActionDto> actions;
    private Map<RecordRef, Set<String>> recordActions;
}
