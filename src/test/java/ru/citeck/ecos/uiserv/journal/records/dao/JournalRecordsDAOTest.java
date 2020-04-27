package ru.citeck.ecos.uiserv.journal.records.dao;

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
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDAO;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.journal.domain.JournalEntity;
import ru.citeck.ecos.uiserv.journal.mapper.JournalMapper;
import ru.citeck.ecos.uiserv.repository.ActionRepository;
import ru.citeck.ecos.uiserv.journal.repository.JournalRepository;

import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class JournalRecordsDAOTest {

    public static final String JOURNAL_DAO_ID = "journal";
    public static final String UISERV_APP_ID = "uiserv";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JournalRepository journalRepository;

    @Autowired
    private ActionRepository actionRepository;

    @BeforeEach
    void setUp() {
        journalRepository.deleteAll();
        actionRepository.deleteAll();
    }

    @Test
    void queryJournalByTypeRef_WithDirectReceiving() throws Exception {

        //  arrange

        JournalEntity journalEntity = new JournalEntity();
        journalEntity.setExtId("myTestJournal");
        journalEntity.setLabel("{\"en\":\"test\"}");
        journalEntity.setTypeRef(TypesDao.testTypeRef.toString());

        journalRepository.save(journalEntity);

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
            .andExpect(jsonPath("$.records[0]",
                is(UISERV_APP_ID + "/" + JOURNAL_DAO_ID + "@" + journalEntity.getExtId())));
    }

    @Test
    void queryJournalByTypeRef_WithParentsIteration() throws Exception {

        //  arrange

        JournalEntity journalEntity = new JournalEntity();
        journalEntity.setExtId("myTestJournal");
        journalEntity.setLabel("{\"en\":\"test\"}");
        journalEntity.setTypeRef(TypesDao.testTypeRef.toString());

        journalRepository.save(journalEntity);

        TypesDao.testTypeRecord.setJournal(null);
        TypesDao.baseTypeRecord.setJournal(TypesDao.journalRef);

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
            .andExpect(jsonPath("$.records[0]", is("uiserv/journal@" + journalEntity.getExtId())));
    }

    @Test
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
            "            \"editorRef\": \"\",\n" +
            "            \"type\": \"text\",\n" +
            "            \"searchable\": true,\n" +
            "            \"sortable\": true,\n" +
            "            \"groupable\": false,\n" +
            "            \"editable\": true,\n" +
            "            \"name\": {\n" +
            "                \"ru\": \"21312\",\n" +
            "                \"en\": \"213123\"\n" +
            "            },\n" +
            "            \"formatter\": {\n" +
            "                \"type\": \"colored\",\n" +
            "                \"config\": {}\n" +
            "            },\n" +
            "            \"show\": true,\n" +
            "            \"visible\": false,\n" +
            "            \"options\": {\n" +
            "                \"type\": \"json\",\n" +
            "                \"config\": [\n" +
            "                    { \"value\": \"value\", \"label\": \"label\" }\n" +
            "                ]\n" +
            "            },\n" +
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
        List<RecordRef> actions = Json.getMapper().read(journalEntity.getActions(), JournalMapper.RecordRefsList.class);
        String actionsString = Json.getMapper().toString(actions);

        journalRepository.save(journalEntity);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/records/query")
            .contentType("application/json")
            .header("Content-type", "application/json")
            .content("{\n" +
                "    \"record\": \"journal@myTestJournal\",\n" +
                "    \"attributes\": [\n" +
                "        \"name\",\n" +
                "        \"typeRef?id\",\n" +
                "        \"predicate\",\n" +
                "        \"editable\",\n" +
                "        \"attributes\",\n" +
                "        \"columns\",\n" +
                "        \"actions[]\",\n" +
                "        \"metaRecord?str\"\n" +
                "    ]\n" +
                "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id",
                is(JOURNAL_DAO_ID + "@" + journalEntity.getExtId())))
            .andExpect(jsonPath("$.attributes.name", is("test")))
            .andExpect(jsonPath("$.attributes.typeRef?id", is(journalEntity.getTypeRef())))
            .andExpect(jsonPath("$.attributes.predicate", is(journalEntity.getPredicate())))
            .andExpect(jsonPath("$.attributes.editable", is(Boolean.FALSE.toString())))
            .andExpect(jsonPath("$.attributes.attributes", is(journalEntity.getAttributes())))
            .andExpect(jsonPath("$.attributes.columns", is(journalEntity.getColumns())))
            .andExpect(jsonPath("$['attributes']['actions[]'][0]", is(actions.get(0).toString())))
            .andExpect(jsonPath("$['attributes']['actions[]'][1]", is(actions.get(1).toString())))
            .andExpect(jsonPath("$.attributes.metaRecord?str", is(journalEntity.getMetaRecord())));
    }

    @Component
    public static class TypesDao extends LocalRecordsDAO implements LocalRecordsMetaDAO<Object> {

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
