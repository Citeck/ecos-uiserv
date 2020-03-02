package ru.citeck.ecos.uiserv.records;

import junit.framework.TestCase;
import lombok.Data;
import lombok.Setter;
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
import ru.citeck.ecos.uiserv.records.evaluator.UserInRoleEvaluator;

import java.util.*;

public class UserInRoleEvaluatorTest extends LocalRecordsDAO implements LocalRecordsMetaDAO<Object> {

    private static final String ID = "userInRoleEvaluatorTest";
    private static final String TEST_USERNAME = "$CURRENT";
    private static final String TEST_WRONG_USERNAME = "WRONG_USER";

    private RecordEvaluatorService evaluatorsService;

    @Before
    public void setup() {
        setId(ID);

        RecordsServiceFactory factory = new RecordsServiceFactory();

        recordsService = factory.getRecordsService();
        recordsService.register(this);

        evaluatorsService = factory.getRecordEvaluatorService();
        evaluatorsService.register(new UserInRoleEvaluator());
    }

    @Test
    public void evaluate() {

        //  arrange
        Map<String, Object> model = new HashMap<>();
        RecordRef userRef = RecordRef.create(ID, "user");
        model.put("user", userRef);

        RecordEvaluatorDto evaluatorDto = new RecordEvaluatorDto();
        evaluatorDto.setType("user-in-role");

        UserInRoleEvaluator.Config config = new UserInRoleEvaluator.Config();
        evaluatorDto.setConfig(JsonUtils.convert(config, ObjectData.class));

        RecordRef recordRef = RecordRef.create(ID, "record");

        TestCaseRole.currentUsername = TEST_USERNAME;

        //  act
        boolean result = evaluatorsService.evaluate(recordRef, evaluatorDto, model);

        //  assert
        Assert.assertTrue(result);
    }

    @Test
    public void evaluateWithWrongUser() {

        //  arrange
        Map<String, Object> model = new HashMap<>();
        RecordRef userRef = RecordRef.create(ID, "user");
        model.put("user", userRef);

        RecordEvaluatorDto evaluatorDto = new RecordEvaluatorDto();
        evaluatorDto.setType("user-in-role");

        UserInRoleEvaluator.Config config = new UserInRoleEvaluator.Config();
        evaluatorDto.setConfig(JsonUtils.convert(config, ObjectData.class));

        RecordRef recordRef = RecordRef.create(ID, "record");

        TestCaseRole.currentUsername = TEST_WRONG_USERNAME;

        //  act
        boolean result = evaluatorsService.evaluate(recordRef, evaluatorDto, model);

        //  assert
        Assert.assertFalse(result);
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

        private static String currentUsername;

        @Override
        public boolean has(String name) {
            return currentUsername.equals(name);
        }
    }

}
