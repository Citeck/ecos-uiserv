package ru.citeck.ecos.uiserv.records;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorDto;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorService;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.objdata.ObjectData;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDAO;
import ru.citeck.ecos.records2.utils.json.JsonUtils;
import ru.citeck.ecos.uiserv.records.evaluator.HasRoleEvaluator;

import java.util.*;
import java.util.stream.Collectors;

public class HasRoleEvaluatorTest extends LocalRecordsDAO implements LocalRecordsMetaDAO<Object> {

    private static final String ID = "hasRoleEvaluatorTest";

    private RecordEvaluatorService evaluatorsService;

    @Before
    public void setup() {
        setId(ID);

        RecordsServiceFactory factory = new RecordsServiceFactory();

        recordsService = factory.getRecordsService();
        recordsService.register(this);

        evaluatorsService = factory.getRecordEvaluatorService();
        evaluatorsService.register(new HasRoleEvaluator());
    }

    @Test
    public void evaluate() {

        //  arrange
        Map<String, Object> model = new HashMap<>();
        RecordRef userRef = RecordRef.create(ID, "user");
        model.put("user", userRef);

        RecordEvaluatorDto evaluatorDto = new RecordEvaluatorDto();
        evaluatorDto.setType("has-role");

        //need this for compare with some role value
        HasRoleEvaluator.Config config = new HasRoleEvaluator.Config();
        config.setRole("ROLE_STR");
        evaluatorDto.setConfig(JsonUtils.convert(config, ObjectData.class));

        RecordRef recordRef = RecordRef.create(ID, "record");

        //  act
        boolean result = evaluatorsService.evaluate(recordRef, evaluatorDto, model);

        //  assert
        Assert.assertTrue(result);
    }

    @Override
    public List<Object> getLocalRecordsMeta(List<RecordRef> list, MetaField metaField) {
       return Collections.singletonList(new TestMixin());
    }

    @Data
    public static class TestMixin implements MetaValue {
        @Override
        public Object getAttribute(String name, MetaField field) {
            return new TestCaseRoles();
        }
    }

    @Data
    public static class TestCaseRoles implements MetaValue {
        @Override
        public Object getAttribute(String name, MetaField field) {
            return new TestCaseRole();
        }
    }

    @Data
    public static class TestCaseRole implements MetaValue {
        @Override
        public boolean has(String name) {
            //  constant that we find some 'true' value for roles
            return true;
        }
    }


}
