package ru.citeck.ecos.uiserv.domain.journal.api.records.dao;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.source.dao.local.InMemRecordsDao;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.ecostype.dto.EcosTypeInfo;
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService;
import ru.citeck.ecos.uiserv.domain.journal.api.records.ResolvedJournalRecordsDao;
import ru.citeck.ecos.uiserv.domain.journal.dto.resolve.ResolvedJournalDef;
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalEntity;
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalRepository;
import ru.citeck.ecos.uiserv.domain.journal.service.mapper.JournalMapper;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes = Application.class)
public class ResolvedJournalRecordsDaoTest {

    @Autowired
    EcosTypeService ecosTypeService;

    @Autowired
    JournalMapper journalMapper;

    @Autowired
    ResolvedJournalRecordsDao testDao;

    @Autowired
    private JournalRepository journalRepository;

    @Autowired
    InMemRecordsDao<EcosTypeInfo> typesSyncRecordsDao;

    final static String TEST_SIMPLE_JOURNAL_ENTITY_JSON = "rJournalDaoTest/simple-journal-entity.json";
    final static String ECOS_TYPE_INFO_JSON = "rJournalDaoTest/ecos-type-info.json";
    final static String TEST_ACTIONS_DEF_JSON = "rJournalDaoTest/actionsDef.json";
    final static String TEST_ACTIONS_JSON = "rJournalDaoTest/actions.json";

    private <T> T getSomethingFromFile(String filePath, Class<T> tClass){
        filePath = getClass().getClassLoader().getResource(filePath).getFile();
        return Json.getMapper().read(new File(filePath), tClass);
    }

    @BeforeEach
    private void setUp() {
        EcosTypeInfo ecosTypeInfo = getSomethingFromFile(ECOS_TYPE_INFO_JSON, EcosTypeInfo.class);
        typesSyncRecordsDao.setRecord(ecosTypeInfo.getId(), ecosTypeInfo);
        journalRepository.deleteAll();
    }

    @Test
    void actionsFromTypeIsNullWithActionsDef() {
        JournalEntity journalEntity = getSomethingFromFile(TEST_SIMPLE_JOURNAL_ENTITY_JSON, JournalEntity.class);
        String actionsDef = getSomethingFromFile(TEST_ACTIONS_DEF_JSON, JsonNode.class).toString();
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
        JournalEntity journalEntity = getSomethingFromFile(TEST_SIMPLE_JOURNAL_ENTITY_JSON, JournalEntity.class);
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
        JournalEntity journalEntity = getSomethingFromFile(TEST_SIMPLE_JOURNAL_ENTITY_JSON, JournalEntity.class);
        String actions = getSomethingFromFile(TEST_ACTIONS_JSON, JsonNode.class).toString();
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
        JournalEntity journalEntity = getSomethingFromFile(TEST_SIMPLE_JOURNAL_ENTITY_JSON, JournalEntity.class);
        String actionsDef = getSomethingFromFile(TEST_ACTIONS_DEF_JSON, JsonNode.class).toString();
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
        JournalEntity journalEntity = getSomethingFromFile(TEST_SIMPLE_JOURNAL_ENTITY_JSON, JournalEntity.class);
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
        JournalEntity journalEntity = getSomethingFromFile(TEST_SIMPLE_JOURNAL_ENTITY_JSON, JournalEntity.class);
        String actions = getSomethingFromFile(TEST_ACTIONS_JSON, JsonNode.class).toString();
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
        JournalEntity journalEntity = getSomethingFromFile(TEST_SIMPLE_JOURNAL_ENTITY_JSON, JournalEntity.class);
        String actionsDef = getSomethingFromFile(TEST_ACTIONS_DEF_JSON, JsonNode.class).toString();
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
        JournalEntity journalEntity = getSomethingFromFile(TEST_SIMPLE_JOURNAL_ENTITY_JSON, JournalEntity.class);
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
        JournalEntity journalEntity = getSomethingFromFile(TEST_SIMPLE_JOURNAL_ENTITY_JSON, JournalEntity.class);
        String actions = getSomethingFromFile(TEST_ACTIONS_JSON, JsonNode.class).toString();
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
}
