package ru.citeck.ecos.uiserv.domain.action;

import ru.citeck.ecos.uiserv.domain.action.dto.EvaluatorDTO;

import static ru.citeck.ecos.uiserv.domain.action.NodeConverter.fromString;
import static ru.citeck.ecos.uiserv.domain.action.NodeConverter.nodeAsString;

/**
 * @author Roman Makarskiy
 */
class EvaluatorDtoFactory {

    static Evaluator fromDto(EvaluatorDTO evaluatorDTO) {
        Evaluator evaluator = new Evaluator();
        evaluator.setId(evaluatorDTO.getId());
        evaluator.setConfigJSON(nodeAsString(evaluatorDTO.getConfig()));
        return evaluator;
    }

    static EvaluatorDTO fromEvaluator(Evaluator evaluator) {
        EvaluatorDTO evaluatorDTO = new EvaluatorDTO();
        evaluatorDTO.setId(evaluator.getId());
        evaluatorDTO.setConfig(fromString(evaluator.getConfigJSON()));
        return evaluatorDTO;
    }

}
