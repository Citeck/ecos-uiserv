package ru.citeck.ecos.uiserv.domain.evaluator.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.atts.value.AttValue;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao;
import ru.citeck.ecos.uiserv.domain.evaluator.RecordEvaluatorDto;
import ru.citeck.ecos.uiserv.domain.evaluator.RecordEvaluatorService;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.uiserv.domain.evaluator.RecordEvaluatorServiceImpl;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserInRoleEvaluatorTest extends AbstractRecordsDao implements RecordAttsDao {

    private static final String ID = "userInRoleEvaluatorTest";
    private static final String TEST_USERNAME = "$CURRENT";
    private static final String TEST_WRONG_USERNAME = "WRONG_USER";

    private RecordEvaluatorServiceImpl evaluatorsService;
    private RecordsServiceFactory factory;

    @BeforeEach
    public void setup() {

        factory = new RecordsServiceFactory();
        recordsService = factory.getRecordsService();
        recordsService.register(this);

        evaluatorsService = new RecordEvaluatorServiceImpl();
        evaluatorsService.setRecordsServiceFactory(factory);
        evaluatorsService.register(new UserInRoleEvaluator());
    }

    @Test
    public void evaluateWithSingleRole() {

        //  arrange
        Map<String, Object> model = new HashMap<>();
        EntityRef userRef = EntityRef.create(ID, "user");
        model.put("user", userRef);

        RecordEvaluatorDto evaluatorDto = new RecordEvaluatorDto();
        evaluatorDto.setType("user-in-role");

        UserInRoleEvaluator.Config config = new UserInRoleEvaluator.Config();
        config.setRole("Admin");
        evaluatorDto.setConfig(Json.getMapper().convert(config, ObjectData.class));

        EntityRef recordRef = EntityRef.create(ID, "record");

        TestCaseRole.currentUsername = TEST_USERNAME;

        //  act
        boolean result = RequestContext.doWithCtxJ(factory, data -> data.withCtxAtts(model), ctx ->
            evaluatorsService.evaluate(recordRef, evaluatorDto));

        //  assert
        assertTrue(result);
    }

    @Test
    public void evaluateWithMultipleRoles() {

        //  arrange
        Map<String, Object> model = new HashMap<>();
        EntityRef userRef = EntityRef.create(ID, "user");
        model.put("user", userRef);

        RecordEvaluatorDto evaluatorDto = new RecordEvaluatorDto();
        evaluatorDto.setType("user-in-role");

        UserInRoleEvaluator.Config config = new UserInRoleEvaluator.Config();
        config.setAnyRole(new HashSet<>(Arrays.asList("Admin", "Initiator")));
        evaluatorDto.setConfig(Json.getMapper().convert(config, ObjectData.class));

        EntityRef recordRef = EntityRef.create(ID, "record");

        TestCaseRole.currentUsername = TEST_USERNAME;

        //  act
        boolean result = RequestContext.doWithCtxJ(factory, data -> data.withCtxAtts(model), ctx ->
            evaluatorsService.evaluate(recordRef, evaluatorDto));

        //  assert
        assertTrue(result);
    }

    @Test
    public void evaluateWithMultipleRolesWithWrongUserInRoles() {

        //  arrange
        Map<String, Object> model = new HashMap<>();
        EntityRef userRef = EntityRef.create(ID, "user");
        model.put("user", userRef);

        RecordEvaluatorDto evaluatorDto = new RecordEvaluatorDto();
        evaluatorDto.setType("user-in-role");

        UserInRoleEvaluator.Config config = new UserInRoleEvaluator.Config();
        config.setAnyRole(new HashSet<>(Arrays.asList("Admin", "Initiator")));
        evaluatorDto.setConfig(Json.getMapper().convert(config, ObjectData.class));

        EntityRef recordRef = EntityRef.create(ID, "record");

        TestCaseRole.currentUsername = TEST_WRONG_USERNAME;

        //  act
        boolean result = RequestContext.doWithCtxJ(factory, data -> data.withCtxAtts(model), ctx ->
            evaluatorsService.evaluate(recordRef, evaluatorDto));

        //  assert
        assertFalse(result);
    }

    @Test
    public void evaluateWithMultipleRolesWithUserNotFoundInRoles() {

        //  arrange
        Map<String, Object> model = new HashMap<>();
        EntityRef userRef = EntityRef.create(ID, "user");
        model.put("user", userRef);

        RecordEvaluatorDto evaluatorDto = new RecordEvaluatorDto();
        evaluatorDto.setType("user-in-role");

        UserInRoleEvaluator.Config config = new UserInRoleEvaluator.Config();
        config.setAnyRole(new HashSet<>(Arrays.asList("Initiator", "Expert")));
        evaluatorDto.setConfig(Json.getMapper().convert(config, ObjectData.class));

        EntityRef recordRef = EntityRef.create(ID, "record");

        TestCaseRole.currentUsername = TEST_USERNAME;

        //  act
        boolean result = RequestContext.doWithCtxJ(factory, data -> data.withCtxAtts(model), ctx ->
            evaluatorsService.evaluate(recordRef, evaluatorDto));

        //  assert
        assertFalse(result);
    }

    @Test
    public void evaluateWithWrongUser() {

        //  arrange
        Map<String, Object> model = new HashMap<>();
        EntityRef userRef = EntityRef.create(ID, "user");
        model.put("user", userRef);

        RecordEvaluatorDto evaluatorDto = new RecordEvaluatorDto();
        evaluatorDto.setType("user-in-role");

        UserInRoleEvaluator.Config config = new UserInRoleEvaluator.Config();
        evaluatorDto.setConfig(Json.getMapper().convert(config, ObjectData.class));

        EntityRef recordRef = EntityRef.create(ID, "record");

        TestCaseRole.currentUsername = TEST_WRONG_USERNAME;

        //  act
        boolean result = RequestContext.doWithCtxJ(factory, data -> data.withCtxAtts(model), ctx ->
            evaluatorsService.evaluate(recordRef, evaluatorDto));

        //  assert
        assertFalse(result);
    }

    @Nullable
    @Override
    public Object getRecordAtts(@NotNull String s) throws Exception {
        return new TestMixin();
    }

    @Data
    public static class TestMixin implements AttValue {

        @Override
        public Object getAtt(String name) {
            return new TestCaseRoles();
        }
    }

    @Data
    public static class TestCaseRoles implements AttValue {

        @Override
        public Object getAtt(String name) {
            return new TestCaseRole(name);
        }
    }


    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @Data
    @AllArgsConstructor
    public static class TestCaseRole implements AttValue {

        private static String currentUsername;
        private String roleName;

        @Override
        public boolean has(String name) {
            return roleName.equals("Admin") && currentUsername.equals(name);
        }
    }

}
