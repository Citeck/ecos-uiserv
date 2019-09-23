package ru.citeck.ecos.uiserv.domain.action;

import ru.citeck.ecos.uiserv.domain.action.dto.ActionDTO;

import static ru.citeck.ecos.uiserv.domain.action.NodeConverter.fromString;
import static ru.citeck.ecos.uiserv.domain.action.NodeConverter.nodeAsString;

/**
 * @author Roman Makarskiy
 */
public class ActionDtoFactory {

    public static ActionDTO fromAction(Action action) {
        ActionDTO dto = new ActionDTO();

        dto.setId(action.getId());
        dto.setTitle(action.getTitle());
        dto.setType(action.getType());
        dto.setIcon(action.getIcon());
        dto.setConfig(fromString(action.getConfigJSON()));

        if (action.getEvaluator() != null) {
            dto.setEvaluator(EvaluatorDtoFactory.fromEvaluator(action.getEvaluator()));
        }

        return dto;
    }

    public static Action fromDto(ActionDTO actionDTO) {
        Action action = new Action();

        action.setId(actionDTO.getId());
        action.setTitle(actionDTO.getTitle());
        action.setType(actionDTO.getType());
        action.setIcon(actionDTO.getIcon());
        action.setConfigJSON(nodeAsString(actionDTO.getConfig()));

        if (actionDTO.getEvaluator() != null) {
            action.setEvaluator(EvaluatorDtoFactory.fromDto(actionDTO.getEvaluator()));
        }

        return action;
    }

}
