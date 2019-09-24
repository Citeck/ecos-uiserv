package ru.citeck.ecos.uiserv.service.action;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.config.UIServProperties;
import ru.citeck.ecos.uiserv.domain.action.ActionDtoFactory;
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDTO;
import ru.citeck.ecos.uiserv.domain.action.dto.EvaluatorDTO;
import ru.citeck.ecos.uiserv.service.evaluator.RecordEvaluatorService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Roman Makarskiy
 */
@Slf4j
@Service
public class ActionService {

    private final RecordEvaluatorService recordEvaluatorService;
    private final RestTemplate alfRestTemplate;
    private final ActionEntityService actionEntityService;

    private final UIServProperties properties;

    @Autowired
    public ActionService(RecordEvaluatorService recordEvaluatorService,
                         @Qualifier("alfrescoRestTemplate") RestTemplate alfRestTemplate,
                         ActionEntityService actionEntityService, UIServProperties properties) {
        this.recordEvaluatorService = recordEvaluatorService;
        this.alfRestTemplate = alfRestTemplate;
        this.actionEntityService = actionEntityService;
        this.properties = properties;
    }

    public List<ActionDTO> getCardActions() {
        //TODO: implement
        return Collections.emptyList();
    }

    public List<ActionDTO> getJournalActions(RecordRef record, String scope) {
        if (StringUtils.isBlank(scope)) {
            throw new IllegalArgumentException("You must specify scope, for journal mode");
        }

        JsonNode forObject = alfRestTemplate.getForObject(properties.getAction().getGetAlfJournalUrlEndpoint() +
            scope, JsonNode.class, new HashMap<>());

        List<ActionDTO> fromAlf = ActionDtoFactory.fromAlfJournalActions(forObject)
            .stream()
            .map(this::merge)
            .collect(Collectors.toList());

        List<ActionDTO> result = CollectionUtils.isNotEmpty(fromAlf) ? fromAlf : getDefaultJournalActions();

        result = result
            .stream()
            .filter(actionDTO -> recordEvaluatorService.evaluate(actionDTO.getEvaluator(), record))
            .collect(Collectors.toList());

        return result;
    }

    private ActionDTO merge(ActionDTO externalAction) {
        String id = externalAction.getId();
        if (StringUtils.isBlank(id)) {
            return externalAction;
        }

        Optional<ActionDTO> servAction = actionEntityService.getById(id);
        if (!servAction.isPresent()) {
            return externalAction;
        }

        ActionDTO action = servAction.get();

        String externalTitle = externalAction.getTitle();
        if (StringUtils.isNotBlank(externalTitle)) {
            action.setTitle(externalTitle);
        }

        String externalType = externalAction.getType();
        if (StringUtils.isNotBlank(externalType)) {
            action.setType(externalType);
        }

        String externalIcon = externalAction.getIcon();
        if (StringUtils.isNotBlank(externalIcon)) {
            action.setIcon(externalIcon);
        }

        JsonNode externalActionConfig = externalAction.getConfig();
        if (externalActionConfig != null && !externalActionConfig.isMissingNode()
            && !externalActionConfig.isNull() && externalActionConfig.size() > 0) {
            action.setConfig(externalActionConfig);
        }

        EvaluatorDTO externalEvaluator = externalAction.getEvaluator();
        if (externalEvaluator != null) {
            action.setEvaluator(externalEvaluator);
        }

        return action;
    }

    private List<ActionDTO> getDefaultJournalActions() {
        return actionEntityService.findAllById(properties.getAction().getDefaultJournalActions());
    }

}
