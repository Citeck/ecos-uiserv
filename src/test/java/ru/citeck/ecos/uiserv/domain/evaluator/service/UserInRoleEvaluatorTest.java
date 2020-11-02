package ru.citeck.ecos.uiserv.domain.evaluator.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorDto;
import ru.citeck.ecos.records2.evaluator.RecordEvaluatorService;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.records3.record.request.RequestContext;

import java.util.*;

public class UserInRoleEvaluatorTest extends LocalRecordsDao implements LocalRecordsMetaDao<Object> {

    private static final String ID = "userInRoleEvaluatorTest";
    private static final String TEST_USERNAME = "$CURRENT";
    private static final String TEST_WRONG_USERNAME = "WRONG_USER";

    private RecordEvaluatorService evaluatorsService;
    private RecordsServiceFactory factory;

    @Before
    public void setup() {
        setId(ID);

        factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();
        recordsService.register(this);

        evaluatorsService = factory.getRecordEvaluatorService();
        evaluatorsService.register(new UserInRoleEvaluator());
    }

    @Test
    public void evaluateWithSingleRole() {

        //  arrange
        Map<String, Object> model = new HashMap<>();
        RecordRef userRef = RecordRef.create(ID, "user");
        model.put("user", userRef);

        RecordEvaluatorDto evaluatorDto = new RecordEvaluatorDto();
        evaluatorDto.setType("user-in-role");

        UserInRoleEvaluator.Config config = new UserInRoleEvaluator.Config();
        config.setRole("Admin");
        evaluatorDto.setConfig(Json.getMapper().convert(config, ObjectData.class));

        RecordRef recordRef = RecordRef.create(ID, "record");

        TestCaseRole.currentUsername = TEST_USERNAME;

        //  act
        boolean result = RequestContext.doWithCtxJ(factory, data -> data.withCtxAtts(model), ctx ->
            evaluatorsService.evaluate(recordRef, evaluatorDto));

        //  assert
        Assert.assertTrue(result);
    }

    @Test
    public void evaluateWithMultipleRoles() {

        //  arrange
        Map<String, Object> model = new HashMap<>();
        RecordRef userRef = RecordRef.create(ID, "user");
        model.put("user", userRef);

        RecordEvaluatorDto evaluatorDto = new RecordEvaluatorDto();
        evaluatorDto.setType("user-in-role");

        UserInRoleEvaluator.Config config = new UserInRoleEvaluator.Config();
        config.setAnyRole(new HashSet<>(Arrays.asList("Admin", "Initiator")));
        evaluatorDto.setConfig(Json.getMapper().convert(config, ObjectData.class));

        RecordRef recordRef = RecordRef.create(ID, "record");

        TestCaseRole.currentUsername = TEST_USERNAME;

        //  act
        boolean result = RequestContext.doWithCtxJ(factory, data -> data.withCtxAtts(model), ctx ->
            evaluatorsService.evaluate(recordRef, evaluatorDto));

        //  assert
        Assert.assertTrue(result);
    }

    @Test
    public void evaluateWithMultipleRolesWithWrongUserInRoles() {

        //  arrange
        Map<String, Object> model = new HashMap<>();
        RecordRef userRef = RecordRef.create(ID, "user");
        model.put("user", userRef);

        RecordEvaluatorDto evaluatorDto = new RecordEvaluatorDto();
        evaluatorDto.setType("user-in-role");

        UserInRoleEvaluator.Config config = new UserInRoleEvaluator.Config();
        config.setAnyRole(new HashSet<>(Arrays.asList("Admin", "Initiator")));
        evaluatorDto.setConfig(Json.getMapper().convert(config, ObjectData.class));

        RecordRef recordRef = RecordRef.create(ID, "record");

        TestCaseRole.currentUsername = TEST_WRONG_USERNAME;

        //  act
        boolean result = RequestContext.doWithCtxJ(factory, data -> data.withCtxAtts(model), ctx ->
            evaluatorsService.evaluate(recordRef, evaluatorDto));

        //  assert
        Assert.assertFalse(result);
    }

    @Test
    public void evaluateWithMultipleRolesWithUserNotFoundInRoles() {

        //  arrange
        Map<String, Object> model = new HashMap<>();
        RecordRef userRef = RecordRef.create(ID, "user");
        model.put("user", userRef);

        RecordEvaluatorDto evaluatorDto = new RecordEvaluatorDto();
        evaluatorDto.setType("user-in-role");

        UserInRoleEvaluator.Config config = new UserInRoleEvaluator.Config();
        config.setAnyRole(new HashSet<>(Arrays.asList("Initiator", "Expert")));
        evaluatorDto.setConfig(Json.getMapper().convert(config, ObjectData.class));

        RecordRef recordRef = RecordRef.create(ID, "record");

        TestCaseRole.currentUsername = TEST_USERNAME;

        //  act
        boolean result = RequestContext.doWithCtxJ(factory, data -> data.withCtxAtts(model), ctx ->
            evaluatorsService.evaluate(recordRef, evaluatorDto));

        //  assert
        Assert.assertFalse(result);
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
        evaluatorDto.setConfig(Json.getMapper().convert(config, ObjectData.class));

        RecordRef recordRef = RecordRef.create(ID, "record");

        TestCaseRole.currentUsername = TEST_WRONG_USERNAME;

        //  act
        boolean result = RequestContext.doWithCtxJ(factory, data -> data.withCtxAtts(model), ctx ->
            evaluatorsService.evaluate(recordRef, evaluatorDto));

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
            return new TestCaseRole(name);
        }
    }

    @Data
    @AllArgsConstructor
    public static class TestCaseRole implements MetaValue {

        private static String currentUsername;
        private String roleName;

        @Override
        public boolean has(String name) {
            return roleName.equals("Admin") && currentUsername.equals(name);
        }
    }

}
