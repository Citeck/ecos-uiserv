package ru.citeck.ecos.uiserv.service.action;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.domain.action.ActionDtoFactory;
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDTO;
import ru.citeck.ecos.uiserv.service.evaluator.RecordEvaluatorService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Roman Makarskiy
 */
@Slf4j
@Service
public class ActionService {

    private static final String ALF_JOURNAL_CONFIG_URL = "/share/proxy/alfresco/api/journals/config?journalId=";

    private final RecordEvaluatorService recordEvaluatorService;
    private final RestTemplate alfRestTemplate;

    @Autowired
    public ActionService(RecordEvaluatorService recordEvaluatorService,
                         @Qualifier("alfrescoRestTemplate") RestTemplate alfRestTemplate) {
        this.recordEvaluatorService = recordEvaluatorService;
        this.alfRestTemplate = alfRestTemplate;
    }

    public List<ActionDTO> getCardActions() {
        //TODO: implement
        return Collections.emptyList();
    }

    public List<ActionDTO> getJournalActions(RecordRef record, String scope) {
        if (StringUtils.isBlank(scope)) {
            throw new IllegalArgumentException("You must specify scope, for journal mode");
        }

        JsonNode forObject = alfRestTemplate.getForObject(ALF_JOURNAL_CONFIG_URL +
            scope, JsonNode.class, new HashMap<>());

        List<ActionDTO> fromAlf = ActionDtoFactory.fromAlfJournalActions(forObject);

        return fromAlf
            .stream()
            .filter(actionDTO -> recordEvaluatorService.evaluate(actionDTO.getEvaluator(), record))
            .collect(Collectors.toList());
    }

}
