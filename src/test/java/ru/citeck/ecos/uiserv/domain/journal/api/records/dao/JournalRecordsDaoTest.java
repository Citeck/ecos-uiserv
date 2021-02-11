package ru.citeck.ecos.uiserv.domain.journal.api.records.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalEntity;
import ru.citeck.ecos.uiserv.domain.journal.service.mapper.JournalMapper;
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalRepository;
import ru.citeck.ecos.uiserv.domain.action.repo.ActionRepository;

import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class JournalRecordsDaoTest {

    public static final String JOURNAL_DAO_ID = "journal";
    public static final String UISERV_APP_ID = "uiserv";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JournalRepository journalRepository;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private RecordsService recordsService;

    @BeforeEach
    void setUp() {
        journalRepository.deleteAll();
        actionRepository.deleteAll();

        recordsService.register(RecordsDaoBuilder.create("emodel/type")
            .addRecord("journal", ObjectData.create())
            .build());
    }

    //@Test
    void queryJournalByTypeRef_WithOtherJournalWithSameParentTypeRef() throws Exception {

        //  arrange

        JournalEntity journalEntity = new JournalEntity();
        journalEntity.setExtId("myTestJournal");
        journalEntity.setLabel("{\"en\":\"test\"}");

        JournalEntity otherJournalEntity = new JournalEntity();
        otherJournalEntity.setExtId("otherTestJournal");
        otherJournalEntity.setLabel("{\"en\":\"test\"}");
        otherJournalEntity.setTypeRef(TypesDao.baseTypeRef.toString());

        journalRepository.save(journalEntity);
        journalRepository.save(otherJournalEntity);

        TypesDao.testTypeRecord.setJournal(null);
        TypesDao.baseTypeRecord.setJournal(null);

        TypesDao.records.put(TypesDao.testTypeRef.getId(), TypesDao.testTypeRecord);
        TypesDao.records.put(TypesDao.baseTypeRecord.getId(), TypesDao.baseTypeRecord);

        //  act and assert

        mockMvc.perform(MockMvcRequestBuilders.post("/api/records/query")
            .contentType("application/json")
            .header("Content-type", "application/json")
            .content("{\n" +
                "    \"query\": {\n" +
                "        \"sourceId\": \"journal\",\n" +
                "        \"query\": {\n" +
                "            \"typeRef\": \"" + TypesDao.testTypeRef.toString() + "\"\n" +
                "                   }" +
                "               }" +
                "     }"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.records[0]", is("uiserv/journal@" + otherJournalEntity.getExtId())));
    }

    @Test
    void queryJournalByTypeRef_WithNotFoundJournal() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.post("/api/records/query")
            .contentType("application/json")
            .header("Content-type", "application/json")
            .content("{\n" +
                "    \"query\": {\n" +
                "        \"sourceId\": \"journal\",\n" +
                "        \"query\": {\n" +
                "            \"typeRef\": \"" + TypesDao.testTypeRef.toString() + "\"\n" +
                "                   }" +
                "               }" +
                "     }"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.records", is(Collections.emptyList())));
    }

    @Test
    void queryJournalMeta() throws Exception {

        JournalEntity journalEntity = new JournalEntity();
        journalEntity.setExtId("myTestJournal");
        journalEntity.setLabel("{\"en\":\"test\"}");
        journalEntity.setTypeRef(TypesDao.testTypeRef.toString());
        journalEntity.setEditable(false);
        journalEntity.setMetaRecord("someAPP/someDAO@MetaRecord");
        journalEntity.setPredicate("{\"att\":\"Type\",\"val\":\"smthg\",\"t\":\"eq\"}");
        journalEntity.setAttributes("{\"a\":\"value\"}");

        journalEntity.setColumns("[\n" +
            "        {\n" +
            "            \"attribute\": \"icase:case\",\n" +
            "            \"name\": \"columnName\",\n" +
            "            \"type\": \"text\",\n" +
            "            \"searchable\": true,\n" +
            "            \"sortable\": true,\n" +
            "            \"groupable\": false,\n" +
            "            \"editable\": true,\n" +
            "            \"label\": {\n" +
            "                \"ru\": \"21312\",\n" +
            "                \"en\": \"213123\"\n" +
            "            },\n" +
            "            \"formatter\": {\n" +
            "                \"type\": \"colored\",\n" +
            "                \"config\": {}\n" +
            "            },\n" +
            "            \"visible\": true,\n" +
            "            \"hidden\": false,\n" +
            "            \"options\": [],\n" +
            "            \"attributes\": {},\n" +
            "            \"filter\": {\n" +
            "                \"type\": \"journal\",\n" +
            "                \"config\": {\n" +
            "                    \"journalId\": \"uiserv/journal@currency\"\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    ]");


        journalEntity.setActions("[\"uiserv/action@testAction\",\"uiserv/action@testAction2\"]");
        List<RecordRef> actions = Json.getMapper().readList(journalEntity.getActions(), RecordRef.class);
        String actionsString = Json.getMapper().toString(actions);

        journalRepository.save(journalEntity);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/records/query")
            .contentType("application/json")
            .header("Content-type", "application/json")
            .content("{\n" +
                "    \"record\": \"journal@myTestJournal\",\n" +
                "    \"attributes\": [\n" +
                "        \"label\",\n" +
                "        \"typeRef?id\",\n" +
                "        \"predicate\",\n" +
                "        \"editable\",\n" +
                "        \"attributes\",\n" +
                "        \"columns[]?json\",\n" +
                "        \"actions[]\",\n" +
                "        \"metaRecord?id\"\n" +
                "    ]\n" +
                "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id",
                is(JOURNAL_DAO_ID + "@" + journalEntity.getExtId())))
            .andExpect(jsonPath("$.attributes.label", is("test")))
            .andExpect(jsonPath("$.attributes.typeRef?id", is(journalEntity.getTypeRef())))
            .andExpect(jsonPath("$.attributes.predicate", is(journalEntity.getPredicate())))
            .andExpect(jsonPath("$.attributes.editable", is(Boolean.FALSE.toString())))
            .andExpect(jsonPath("$.attributes.attributes", is(journalEntity.getAttributes())))
            //.andExpect(jsonPath("$.attributes.columns", is(journalEntity.getColumns())))
            //.andExpect(jsonPath("$['attributes']['actions[]'][0]", is(actions.get(0).toString())))
            //.andExpect(jsonPath("$['attributes']['actions[]'][1]", is(actions.get(1).toString())))
            .andExpect(jsonPath("$.attributes.metaRecord?id", is(journalEntity.getMetaRecord())));
    }

    @Component
    public static class TypesDao extends LocalRecordsDao implements LocalRecordsMetaDao<Object> {

        public static final String EMODEL_APP_ID = "emodel";
        public static final String TYPE_DAO_ID = "type";

        public static final RecordRef journalRef = RecordRef.create(UISERV_APP_ID, JOURNAL_DAO_ID, "myTestJournal");

        public static final RecordRef testTypeRef = RecordRef.create(EMODEL_APP_ID, TYPE_DAO_ID, "testType");
        public static final RecordRef baseTypeRef = RecordRef.create(EMODEL_APP_ID, TYPE_DAO_ID, "base");

        public static final TypeRecord testTypeRecord = new TypeRecord(
            "testType",
            Collections.singletonList(baseTypeRef),
            journalRef
        );
        public static final TypeRecord baseTypeRecord = new TypeRecord(
            "baseType",
            Collections.emptyList(),
            null
        );

        private static Map<String, TypeRecord> records = new HashMap<>();

        static {
            records.put(testTypeRef.getId(), testTypeRecord);
            records.put(baseTypeRef.getId(), baseTypeRecord);
        }

        public TypesDao() {
            super(false);
            setId(TYPE_DAO_ID);
        }

        @Override
        public List<Object> getLocalRecordsMeta(List<RecordRef> records, MetaField metaField) {
            return records.stream()
                .map(r -> TypesDao.records.get(r.getId()))
                .collect(Collectors.toList());
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class TypeRecord implements MetaValue {

            private String id;
            private List<RecordRef> parents = new ArrayList<>();
            private RecordRef journal;

            @Override
            public Object getAttribute(String name, MetaField field) {
                switch (name) {
                    case "parents":
                        return this.parents;
                    case "journal":
                        return this.journal;
                }
                return null;
            }
        }
    }
}
