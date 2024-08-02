package ru.citeck.ecos.uiserv.domain.evaluator.service;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.atts.value.AttValue;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao;
import ru.citeck.ecos.uiserv.domain.evaluator.RecordEvaluatorDto;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.uiserv.domain.evaluator.RecordEvaluatorServiceImpl;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class IsAdminEvaluatorTest extends AbstractRecordsDao implements RecordAttsDao {

    private static final String ID = "isAdminEvaluatorTest";

    private RecordEvaluatorServiceImpl evaluatorsService;
    private RecordsServiceFactory factory = new RecordsServiceFactory();

    @BeforeEach
    public void setup() {

        recordsService = factory.getRecordsService();
        recordsService.register(this);

        evaluatorsService = new RecordEvaluatorServiceImpl();
        evaluatorsService.setRecordsServiceFactory(factory);
        evaluatorsService.register(new IsAdminEvaluator());
    }

    @Test
    public void evaluate() {

        //  arrange
        Map<String, Object> model = new HashMap<>();
        EntityRef userRef = EntityRef.create(ID, "user");
        model.put("user", userRef);

        RecordEvaluatorDto evaluatorDto = new RecordEvaluatorDto();
        evaluatorDto.setType("is-admin");

        EntityRef recordRef = EntityRef.create(ID, "documentId");

        //  act
        boolean result = RequestContext.doWithCtxJ(factory, data -> data.withCtxAtts(model), ctx ->
            evaluatorsService.evaluate(recordRef, evaluatorDto));

        //  assert
        assertTrue(result);
    }

    @Nullable
    @Override
    public Object getRecordAtts(@NotNull String recordId) throws Exception {
        return new TestUserRecord();
    }

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @Data
    private static class TestUserRecord implements AttValue {

        private boolean isAdmin = true;

        @Override
        public Object getAtt(@NotNull String name) {
            return isAdmin;
        }
    }
}
