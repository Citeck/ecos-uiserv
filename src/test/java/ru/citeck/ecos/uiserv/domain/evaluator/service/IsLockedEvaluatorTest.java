package ru.citeck.ecos.uiserv.domain.evaluator.service;

import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorDto;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorService;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IsLockedEvaluatorTest extends LocalRecordsDao implements LocalRecordsMetaDao<Object> {

    private static final String ID = "isLockedEvaluatorTest";

    private RecordEvaluatorService evaluatorsService;
    private RecordsServiceFactory factory = new RecordsServiceFactory();

    @BeforeEach
    public void setup() {
        setId(ID);

        recordsService = factory.getRecordsService();
        recordsService.register(this);

        evaluatorsService = factory.getRecordEvaluatorService();
        evaluatorsService.register(new IsLockedEvaluator());
    }

    @Override
    public List<Object> getLocalRecordsMeta(List<EntityRef> list, MetaField metaField) {
        return Collections.singletonList(new TestRecord());
    }

    @Data
    private static class TestRecord implements MetaValue {

        private static boolean isLocked = true;

        @Override
        public Object getAttribute(String name, MetaField field) {
            return isLocked;
        }
    }

    @Test
    public void evaluateIsLocked() {
        //  arrange
        Map<String, Object> model = new HashMap<>();
        RecordRef userRef = RecordRef.create(ID, "user");
        model.put("user", userRef);

        RecordEvaluatorDto evaluatorDto = new RecordEvaluatorDto();
        evaluatorDto.setType("is-locked");

        RecordRef recordRef = RecordRef.create(ID, "documentId");

        //  act

        boolean result = RequestContext.doWithCtxJ(factory, data -> data.withCtxAtts(model), ctx ->
            evaluatorsService.evaluate(recordRef, evaluatorDto));

        //  assert
        assertTrue(result);
    }

    @Test
    public void evaluateIsNotLocked() {
        //  arrange
        Map<String, Object> model = new HashMap<>();
        RecordRef userRef = RecordRef.create(ID, "user");
        model.put("user", userRef);

        RecordEvaluatorDto evaluatorDto = new RecordEvaluatorDto();
        evaluatorDto.setType("is-locked");

        TestRecord.isLocked = false;

        RecordRef recordRef = RecordRef.create(ID, "documentId");

        //  act
        boolean result = RequestContext.doWithCtxJ(factory, data -> data.withCtxAtts(model), ctx ->
            evaluatorsService.evaluate(recordRef, evaluatorDto));

        //  assert
        assertFalse(result);
    }
}
