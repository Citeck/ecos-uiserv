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
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.uiserv.config.UiServProperties;
import ru.citeck.ecos.uiserv.service.evaluator.RecordEvaluatorService;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Roman Makarskiy
 */
@Slf4j
@Service
public class ActionService {

   /* private static final String RECORD_ACTIONS_ATT_SCHEMA = "_actions?json";
    private static final String DEFAULT_SOURCE_ID = "alfresco";

    private final RecordsService recordsService;
    private final RecordEvaluatorService recordEvaluatorService;
    private final RestTemplate alfRestTemplate;
    private final ActionEntityService actionEntityService;

    private final UiServProperties properties;

    @Autowired
    public ActionService(RecordsService recordsService, RecordEvaluatorService recordEvaluatorService,
                         @Qualifier("alfrescoRestTemplate") RestTemplate alfRestTemplate,
                         ActionEntityService actionEntityService, UiServProperties properties) {
        this.recordsService = recordsService;
        this.recordEvaluatorService = recordEvaluatorService;
        this.alfRestTemplate = alfRestTemplate;
        this.actionEntityService = actionEntityService;
        this.properties = properties;
    }

    public List<ActionDto> getDefaultJournalActions() {
        return actionEntityService.findAllById(properties.getAction().getDefaultJournalActions());
    }

    public List<ActionDto> getCardActions(RecordRef record) {
        JsonNode attribute = recordsService.getAttribute(setDefaultSourceId(record), RECORD_ACTIONS_ATT_SCHEMA);
        return ActionDtoFactory.fromJsonNode(attribute);
    }

    public List<ActionDto> getJournalActions(RecordRef record, String scope) {
        if (StringUtils.isBlank(scope)) {
            throw new IllegalArgumentException("You must specify scope, for journal mode");
        }

        JsonNode forObject = alfRestTemplate.getForObject(properties.getAction().getGetAlfJournalUrlEndpoint() +
            scope, JsonNode.class, new HashMap<>());

        List<ActionDto> fromAlf = ActionDtoFactory.fromAlfJournalActions(forObject)
            .stream()
            .map(this::merge)
            .collect(Collectors.toList());

        List<ActionDto> result = CollectionUtils.isNotEmpty(fromAlf) ? fromAlf : getDefaultJournalActions();

        return result
            .stream()
            .filter(ActionDto -> recordEvaluatorService.evaluate(ActionDto.getEvaluator(), setDefaultSourceId(record)))
            .collect(Collectors.toList());
    }

    private RecordRef setDefaultSourceId(RecordRef record) {
        if (StringUtils.isNotBlank(record.getSourceId())) {
            return record;
        }
        return RecordRef.create(record.getAppName(), DEFAULT_SOURCE_ID, record.getId());
    }

    private ActionDto merge(ActionDto externalAction) {
        String id = externalAction.getId();
        if (StringUtils.isBlank(id)) {
            return externalAction;
        }

        Optional<ActionDto> servAction = actionEntityService.getById(id);
        if (!servAction.isPresent()) {
            return externalAction;
        }

        ActionDto action = servAction.get();

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

        EvaluatorDto externalEvaluator = externalAction.getEvaluator();
        if (externalEvaluator != null) {
            action.setEvaluator(externalEvaluator);
        }

        return action;
    }
*/
}
