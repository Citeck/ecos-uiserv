package ru.citeck.ecos.uiserv.domain.evaluator.service;

import lombok.Data;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorDto;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorService;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.uiserv.domain.evaluator.service.IsLockedEvaluator;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IsLockedEvaluatorTest extends LocalRecordsDao implements LocalRecordsMetaDao<Object> {

    private static final String ID = "isLockedEvaluatorTest";

    private RecordEvaluatorService evaluatorsService;

    @Before
    public void setup() {
        setId(ID);

        RecordsServiceFactory factory = new RecordsServiceFactory();

        recordsService = factory.getRecordsService();
        recordsService.register(this);

        evaluatorsService = factory.getRecordEvaluatorService();
        evaluatorsService.register(new IsLockedEvaluator());
    }

    @Override
    public List<Object> getLocalRecordsMeta(List<RecordRef> list, MetaField metaField) {
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
        boolean result = evaluatorsService.evaluate(recordRef, evaluatorDto, model);

        //  assert
        Assert.assertTrue(result);
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
        boolean result = evaluatorsService.evaluate(recordRef, evaluatorDto, model);

        //  assert
        Assert.assertFalse(result);
    }
}
