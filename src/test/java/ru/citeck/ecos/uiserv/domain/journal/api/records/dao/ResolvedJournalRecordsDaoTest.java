package ru.citeck.ecos.uiserv.domain.journal.api.records.dao;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import org.apache.commons.lang3.LocaleUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.reflection.FieldSetter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.source.dao.local.InMemRecordsDao;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.ecostype.dto.EcosTypeInfo;
import ru.citeck.ecos.uiserv.domain.journal.api.records.ResolvedJournalRecordsDao;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalColumnDef;
import ru.citeck.ecos.uiserv.domain.journal.dto.resolve.ResolvedColumnDef;
import ru.citeck.ecos.uiserv.domain.journal.dto.resolve.ResolvedJournalDef;
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalEntity;
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalRepository;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = Application.class)
public class ResolvedJournalRecordsDaoTest {

    @Autowired
    private ResolvedJournalRecordsDao testDao;

    @Autowired
    private JournalRepository journalRepository;

    @Autowired
    private InMemRecordsDao<EcosTypeInfo> typesSyncRecordsDao;

    private JournalEntity journalEntity;

    private final static String TEST_SIMPLE_JOURNAL_ENTITY_JSON = "ResolvedJournalRecordsDaoTest/simple-journal-entity.json";
    private final static String ECOS_TYPE_INFO_JSON = "ResolvedJournalRecordsDaoTest/ecos-type-info.json";
    private final static String TEST_ACTIONS_DEF_JSON = "ResolvedJournalRecordsDaoTest/actionsDef.json";
    private final static String TEST_ACTIONS_JSON = "ResolvedJournalRecordsDaoTest/actions.json";
    private final static String TEST_RECORDS_COL_ANSWER_JSON = "ResolvedJournalRecordsDaoTest/records-col-answer.json";

    private final String actions = getSomethingFromFile(TEST_ACTIONS_JSON, JsonNode.class).toString();
    private final String actionsDef = getSomethingFromFile(TEST_ACTIONS_DEF_JSON, JsonNode.class).toString();

    private <T> T getSomethingFromFile(String filePath, Class<T> tClass){
        filePath = getClass().getClassLoader().getResource(filePath).getFile();
        return Json.getMapper().read(new File(filePath), tClass);
    }

    @BeforeEach
    private void setUp() {
        EcosTypeInfo ecosTypeInfo = getSomethingFromFile(ECOS_TYPE_INFO_JSON, EcosTypeInfo.class);
        typesSyncRecordsDao.setRecord(ecosTypeInfo.getId(), ecosTypeInfo);
        journalRepository.deleteAll();

        journalEntity = getSomethingFromFile(TEST_SIMPLE_JOURNAL_ENTITY_JSON, JournalEntity.class);
    }

    @Test
    void actionsFromTypeIsNullWithActionsDef() {
        journalEntity.setActionsFromType(null);
        journalEntity.setActionsDef(actionsDef);
        journalEntity.setActions("");
        journalRepository.save(journalEntity);

        ResolvedJournalDef recordAtts = (ResolvedJournalDef) testDao.getRecordAtts(journalEntity.getExtId());
        List<RecordRef> expected = Collections.singletonList(
            RecordRef.valueOf("uiserv/action@journal$simple-journal$group-action-def")
        );
        List<RecordRef> actionsList = recordAtts.getJournalDef().getActions();
        assertThat(actionsList).isEqualTo(expected);
    }

    @Test
    void actionsFromTypeIsNullWithoutActions() {
        journalEntity.setActionsFromType(null);
        journalEntity.setActionsDef("");
        journalEntity.setActions("");
        journalRepository.save(journalEntity);

        ResolvedJournalDef recordAtts = (ResolvedJournalDef) testDao.getRecordAtts(journalEntity.getExtId());
        List<RecordRef> expected = Arrays.asList(
            RecordRef.valueOf("uiserv/action@edit"),
            RecordRef.valueOf("uiserv/action@delete")
        );
        List<RecordRef> actionsList = recordAtts.getJournalDef().getActions();
        assertThat(actionsList).isEqualTo(expected);
    }

    @Test
    void actionsFromTypeIsNullWithActions() {
        journalEntity.setActionsFromType(null);
        journalEntity.setActionsDef("");
        journalEntity.setActions(actions);
        journalRepository.save(journalEntity);

        ResolvedJournalDef recordAtts = (ResolvedJournalDef) testDao.getRecordAtts(journalEntity.getExtId());
        List<RecordRef> expected = Arrays.asList(
            RecordRef.valueOf("uiserv/action@ecos-module-download"),
            RecordRef.valueOf("uiserv/action@edit"),
            RecordRef.valueOf("uiserv/action@module-copy")
        );
        List<RecordRef> actionsList = recordAtts.getJournalDef().getActions();
        assertThat(actionsList).isEqualTo(expected);
    }

    @Test
    void actionsFromTypeIsFalseWithActionsDef() {
        journalEntity.setActionsFromType(false);
        journalEntity.setActionsDef(actionsDef);
        journalEntity.setActions("");
        journalRepository.save(journalEntity);

        ResolvedJournalDef recordAtts = (ResolvedJournalDef) testDao.getRecordAtts(journalEntity.getExtId());
        List<RecordRef> expected = Collections.singletonList(
            RecordRef.valueOf("uiserv/action@journal$simple-journal$group-action-def")
        );
        List<RecordRef> actionsList = recordAtts.getJournalDef().getActions();
        assertThat(actionsList).isEqualTo(expected);
    }

    @Test
    void actionsFromTypeIsFalseWithoutActions() {
        journalEntity.setActionsFromType(false);
        journalEntity.setActionsDef("");
        journalEntity.setActions("");
        journalRepository.save(journalEntity);

        ResolvedJournalDef recordAtts = (ResolvedJournalDef) testDao.getRecordAtts(journalEntity.getExtId());
        List<RecordRef> expected = Collections.emptyList();
        List<RecordRef> actionsList = recordAtts.getJournalDef().getActions();
        assertThat(actionsList).isEqualTo(expected);
    }

    @Test
    void actionsFromTypeIsFalseWithActions() {
        journalEntity.setActionsFromType(false);
        journalEntity.setActionsDef("");
        journalEntity.setActions(actions);
        journalRepository.save(journalEntity);

        ResolvedJournalDef recordAtts = (ResolvedJournalDef) testDao.getRecordAtts(journalEntity.getExtId());
        List<RecordRef> expected = Arrays.asList(
            RecordRef.valueOf("uiserv/action@ecos-module-download"),
            RecordRef.valueOf("uiserv/action@edit"),
            RecordRef.valueOf("uiserv/action@module-copy")
        );
        List<RecordRef> actionsList = recordAtts.getJournalDef().getActions();
        assertThat(actionsList).isEqualTo(expected);
    }

    @Test
    void actionsFromTypeIsTrueWithActionsDef() {
        journalEntity.setActionsFromType(true);
        journalEntity.setActionsDef(actionsDef);
        journalEntity.setActions("");
        journalRepository.save(journalEntity);

        ResolvedJournalDef recordAtts = (ResolvedJournalDef) testDao.getRecordAtts(journalEntity.getExtId());
        List<RecordRef> expected = Arrays.asList(
            RecordRef.valueOf("uiserv/action@journal$simple-journal$group-action-def"),
            RecordRef.valueOf("uiserv/action@edit"),
            RecordRef.valueOf("uiserv/action@delete")
        );
        List<RecordRef> actionsList = recordAtts.getJournalDef().getActions();
        assertThat(actionsList).isEqualTo(expected);
    }

    @Test
    void actionsFromTypeIsTrueWithoutActions() {
        journalEntity.setActionsFromType(true);
        journalEntity.setActionsDef("");
        journalEntity.setActions("");
        journalRepository.save(journalEntity);

        ResolvedJournalDef recordAtts = (ResolvedJournalDef) testDao.getRecordAtts(journalEntity.getExtId());
        List<RecordRef> expected = Arrays.asList(
            RecordRef.valueOf("uiserv/action@edit"),
            RecordRef.valueOf("uiserv/action@delete")
        );
        List<RecordRef> actionsList = recordAtts.getJournalDef().getActions();
        assertThat(actionsList).isEqualTo(expected);
    }

    @Test
    void actionsFromTypeIsTrueWithActions() {
        journalEntity.setActionsFromType(true);
        journalEntity.setActionsDef("");
        journalEntity.setActions(actions);
        journalRepository.save(journalEntity);

        ResolvedJournalDef recordAtts = (ResolvedJournalDef) testDao.getRecordAtts(journalEntity.getExtId());
        List<RecordRef> expected = Arrays.asList(
            RecordRef.valueOf("uiserv/action@ecos-module-download"),
            RecordRef.valueOf("uiserv/action@edit"),
            RecordRef.valueOf("uiserv/action@module-copy"),
            RecordRef.valueOf("uiserv/action@delete")
        );
        List<RecordRef> actionsList = recordAtts.getJournalDef().getActions();
        assertThat(actionsList).isEqualTo(expected);
    }

    @Test
    void columnTestEval() throws Exception {
        journalRepository.save(journalEntity);

        RecordAtts colsAtts = new RecordAtts("alfresco/@");
        JsonNode testData = getSomethingFromFile(TEST_RECORDS_COL_ANSWER_JSON, JsonNode.class);
        colsAtts.setAttributes(ObjectData.create(testData));

        // Mock recordsService
        RecordsService recordsServiceMock = mock(RecordsService.class);
        when(recordsServiceMock.getAtts(eq(RecordRef.valueOf("alfresco/@")), anyMap()))
            .thenReturn(colsAtts);

        // Set recordsServiceMock in testDao
        FieldSetter.setField(testDao, AbstractRecordsDao.class.getDeclaredField("recordsService") ,recordsServiceMock);

        ResolvedJournalDef recordAtts = (ResolvedJournalDef) testDao.getRecordAtts(journalEntity.getExtId());
        List<ResolvedColumnDef> invoke = recordAtts.getColumnsEval().invoke();
        JournalColumnDef testCol = invoke.get(2).getColumn();

        assertAll(
            () -> assertThat(testCol.getId()).isEqualTo("test-column1"),
            () -> assertThat(testCol.getAttribute()).isEqualTo("test-column1"),
            () -> assertThat(testCol.getName()).isEqualTo(new MLText()
                .withValue(LocaleUtils.toLocale("ru"), "Тестовый столбец молодец")
                .withValue(LocaleUtils.toLocale("en"), "Testovii stolbec molodec")),
            () -> assertThat(testCol.getType()).isEqualTo(AttributeType.ASSOC),
            () -> assertThat(testCol.getEditor().getType()).isEqualTo("journal"),
            () -> assertThat(testCol.getEditor().getConfig().get("journalId").asText()).isEqualTo("simple-journal")
        );
    }
}
