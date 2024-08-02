package ru.citeck.ecos.uiserv.domain.evaluator.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.uiserv.domain.evaluator.RecordEvaluatorServiceImpl;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserInGroupEvaluatorTest extends AbstractRecordsDao implements RecordAttsDao {

    private static final String ID = "userInGroupEvaluatorTest";
    private RecordEvaluatorServiceImpl evaluatorsService;
    private final RecordsServiceFactory factory = new RecordsServiceFactory();

    @BeforeEach
    public void setup() {

        recordsService = factory.getRecordsService();
        recordsService.register(this);

        evaluatorsService = new RecordEvaluatorServiceImpl();
        evaluatorsService.setRecordsServiceFactory(factory);
        evaluatorsService.register(new UserInGroupEvaluator());
    }

    @Test
    public void evaluate_nullGroupInConfig_returnFalse() {
        Map<String, Object> model = new HashMap<>();
        EntityRef userRef = EntityRef.create(ID, "user");
        model.put("user", userRef);

        RecordEvaluatorDto evaluatorDto = new RecordEvaluatorDto();
        evaluatorDto.setType("user-in-group");

        UserInGroupEvaluator.Config config = new UserInGroupEvaluator.Config();
        config.setGroupName(null);
        evaluatorDto.setConfig(Json.getMapper().convert(config, ObjectData.class));

        EntityRef recordRef = EntityRef.create(ID, "record");

        TestUserAuthorities.userAuthorities = Collections.emptyList();

        RequestContext.doWithCtxJ(factory, data -> data.withCtxAtts(model), ctx -> {
            assertFalse(evaluatorsService.evaluate(recordRef, evaluatorDto));
            return null;
        });
    }

    @Test
    public void evaluate_oneGroupInConfig_userHasNotRole_returnFalse() {
        Map<String, Object> model = new HashMap<>();
        EntityRef userRef = EntityRef.create(ID, "user");
        model.put("user", userRef);

        RecordEvaluatorDto evaluatorDto = new RecordEvaluatorDto();
        evaluatorDto.setType("user-in-group");

        UserInGroupEvaluator.Config config = new UserInGroupEvaluator.Config();
        config.setGroupName(Collections.singletonList("notIncludedRole"));
        evaluatorDto.setConfig(Json.getMapper().convert(config, ObjectData.class));

        EntityRef recordRef = EntityRef.create(ID, "record");

        TestUserAuthorities.userAuthorities = Collections.singletonList("userRole");

        RequestContext.doWithCtxJ(factory, data -> data.withCtxAtts(model), ctx -> {
            assertFalse(evaluatorsService.evaluate(recordRef, evaluatorDto));
            return null;
        });
    }

    @Test
    public void evaluate_oneGroupInConfig_userHasRole_returnTrue() {
        Map<String, Object> model = new HashMap<>();
        EntityRef userRef = EntityRef.create(ID, "user");
        model.put("user", userRef);

        RecordEvaluatorDto evaluatorDto = new RecordEvaluatorDto();
        evaluatorDto.setType("user-in-group");

        UserInGroupEvaluator.Config config = new UserInGroupEvaluator.Config();
        String includedRole = "includedRole";
        config.setGroupName(Collections.singletonList(includedRole));
        evaluatorDto.setConfig(Json.getMapper().convert(config, ObjectData.class));

        EntityRef recordRef = EntityRef.create(ID, "record");

        TestUserAuthorities.userAuthorities = Arrays.asList(
            includedRole,
            "anotherRole"
        );

        RequestContext.doWithCtxJ(factory, data -> data.withCtxAtts(model), ctx -> {
            assertTrue(evaluatorsService.evaluate(recordRef, evaluatorDto));
            return null;
        });
    }

    @Test
    public void evaluate_multipleGroupInConfig_userHasOneOfRoles_returnTrue() {
        Map<String, Object> model = new HashMap<>();
        EntityRef userRef = EntityRef.create(ID, "user");
        model.put("user", userRef);

        RecordEvaluatorDto evaluatorDto = new RecordEvaluatorDto();
        evaluatorDto.setType("user-in-group");

        UserInGroupEvaluator.Config config = new UserInGroupEvaluator.Config();
        String includedRole = "includedRole";
        config.setGroupName(Arrays.asList(
            includedRole,
            "notIncluded1",
            "notIncluded2"
        ));
        evaluatorDto.setConfig(Json.getMapper().convert(config, ObjectData.class));

        EntityRef recordRef = EntityRef.create(ID, "record");

        TestUserAuthorities.userAuthorities = Arrays.asList(
            includedRole,
            "includedRole1"
        );

        RequestContext.doWithCtxJ(factory, data -> data.withCtxAtts(model), ctx -> {
            assertTrue(evaluatorsService.evaluate(recordRef, evaluatorDto));
            return null;
        });
    }

    @Nullable
    @Override
    public Object getRecordAtts(@NotNull String s) throws Exception {
        return new TestUserRecord();
    }

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @Data
    @NoArgsConstructor
    private static class TestUserRecord implements AttValue {
        @Override
        public Object getAtt(@NotNull String name) {
            return new TestUserAuthorities();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class TestUserAuthorities implements AttValue {

        private static List<String> userAuthorities = new ArrayList<>();
        private List<String> authorities = new ArrayList<>();

        @Override
        public boolean has(@NotNull String name) {
            return userAuthorities.contains(name);
        }
    }
}
