package ru.citeck.ecos.uiserv.domain.evaluator.evaluators;

import lombok.Data;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.ServiceFactoryAware;
import ru.citeck.ecos.records2.predicate.element.elematts.RecordAttsElement;
import ru.citeck.ecos.uiserv.domain.evaluator.RecordEvaluator;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.PredicateUtils;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts;

import java.util.List;

public class PredicateEvaluator implements RecordEvaluator<List<String>, RecordAtts, PredicateEvaluator.Config>,
                                           ServiceFactoryAware {

    public static final String TYPE = "predicate";

    private PredicateService predicateService;

    @Override
    public List<String> getMetaToRequest(Config config) {
        Predicate predicate = Json.getMapper().convert(config.predicate, Predicate.class);
        return PredicateUtils.getAllPredicateAttributes(predicate);
    }

    @Override
    public boolean evaluate(RecordAtts meta, Config config) {
        Predicate predicate = Json.getMapper().convert(config.predicate, Predicate.class);
        if (predicate != null) {
            predicate = PredicateUtils.resolvePredicateWithAttributes(predicate, meta.getAtts());
        } else {
            return false;
        }
        return predicateService.isMatch(RecordAttsElement.create(new RecordAtts(meta)), predicate);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void setRecordsServiceFactory(RecordsServiceFactory serviceFactory) {
        this.predicateService = serviceFactory.getPredicateService();
    }

    @Data
    public static class Config {
        private Predicate predicate;
    }
}
