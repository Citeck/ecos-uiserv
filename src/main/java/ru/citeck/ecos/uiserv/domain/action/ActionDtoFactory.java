package ru.citeck.ecos.uiserv.domain.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDTO;
import ru.citeck.ecos.uiserv.domain.action.dto.EvaluatorDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static ru.citeck.ecos.uiserv.domain.action.NodeConverter.fromString;
import static ru.citeck.ecos.uiserv.domain.action.NodeConverter.nodeAsString;

/**
 * @author Roman Makarskiy
 */
public class ActionDtoFactory {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .addMixIn(ActionDTO.class, ParamsActionMixIn.class)
        .addMixIn(EvaluatorDTO.class, ParamsActionMixIn.class);

    private static final String PARAM_META = "meta";
    private static final String PARAM_ACTIONS = "actions";

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

    public static List<ActionDTO> fromAlfJournalActions(JsonNode node) {
        if (node == null) {
            return Collections.emptyList();
        }

        JsonNode meta = node.get(PARAM_META);

        if (meta == null || meta.isMissingNode() || meta.isNull()) {
            return Collections.emptyList();
        }

        JsonNode actionsNode = meta.get(PARAM_ACTIONS);
        if (actionsNode.isMissingNode() || actionsNode.isNull() || actionsNode.size() == 0) {
            return Collections.emptyList();
        }

        ActionDTO[] actions;

        try {
            actions = OBJECT_MAPPER.treeToValue(actionsNode, ActionDTO[].class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed convert alf action to ActionDTO", e);
        }

        return new ArrayList<>(Arrays.asList(actions));
    }

}
