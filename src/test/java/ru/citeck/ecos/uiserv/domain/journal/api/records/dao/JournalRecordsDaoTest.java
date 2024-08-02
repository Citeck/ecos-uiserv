package ru.citeck.ecos.uiserv.domain.journal.api.records.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.record.atts.value.AttValue;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.action.repo.ActionRepository;
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalEntity;
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalRepository;
import ru.citeck.ecos.webapp.api.entity.EntityRef;
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EcosSpringExtension.class)
@SpringBootTest(classes = Application.class)
public class JournalRecordsDaoTest {

    public static final String JOURNAL_DAO_ID = "journal";
    public static final String UISERV_APP_ID = "uiserv";

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
    }

    @Test
    void queryJournalByTypeRef_WithNotFoundJournal() {

        val res = recordsService.query(RecordsQuery.create()
            .withSourceId("journal")
            .withQuery(DataValue.createObj().set("typeRef", TypesDao.testTypeRef.toString()))
            .build()
        );
        assertThat(res.getRecords()).isEmpty();
    }

    @Test
    void queryJournalMeta() {

        JournalEntity journalEntity = new JournalEntity();
        journalEntity.setExtId("myTestJournal");
        journalEntity.setName("{\"en\":\"test\"}");
        journalEntity.setTypeRef(TypesDao.testTypeRef.toString());
        journalEntity.setEditable(false);
        journalEntity.setPredicate("{\"t\":\"eq\",\"att\":\"Type\",\"val\":\"smthg\"}");
        journalEntity.setAttributes("{\"a\":\"value\"}");

        journalEntity.setColumns("[\n" +
            "        {\n" +
            "            \"attribute\": \"icase:case\",\n" +
            "            \"id\": \"columnName\",\n" +
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

        journalRepository.save(journalEntity);

        val recAtts = recordsService.getAtts(JOURNAL_DAO_ID +"@myTestJournal", List.of(
            "name",
            "typeRef?id",
            "predicate",
            "editable",
            "properties",
            "columns[]?json",
            "actions[]",
            "metaRecord?id"
        ));

        assertThat(recAtts.getId().toString()).isEqualTo("uiserv/" + JOURNAL_DAO_ID + "@" + journalEntity.getExtId());
        assertThat(recAtts.get("name").asText()).isEqualTo("test");
        assertThat(recAtts.get("typeRef?id").asText()).isEqualTo(journalEntity.getTypeRef());
        assertThat(recAtts.get("predicate").asText()).isEqualTo(journalEntity.getPredicate());
        assertThat(recAtts.get("editable").asText()).isEqualTo(Boolean.FALSE.toString());
        assertThat(recAtts.get("properties").asText()).isEqualTo(journalEntity.getAttributes());
    }

    @Component
    public static class TypesDao extends AbstractRecordsDao implements RecordAttsDao {

        public static final String EMODEL_APP_ID = "emodel";
        public static final String TYPE_DAO_ID = "type";

        public static final EntityRef journalRef = EntityRef.create(UISERV_APP_ID, JOURNAL_DAO_ID, "myTestJournal");

        public static final EntityRef testTypeRef = EntityRef.create(EMODEL_APP_ID, TYPE_DAO_ID, "testType");
        public static final EntityRef baseTypeRef = EntityRef.create(EMODEL_APP_ID, TYPE_DAO_ID, "base");

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
            records.put(testTypeRef.getLocalId(), testTypeRecord);
            records.put(baseTypeRef.getLocalId(), baseTypeRecord);
        }

        @Nullable
        @Override
        public Object getRecordAtts(@NotNull String recordId) throws Exception {
            return TypesDao.records.get(recordId);
        }

        @NotNull
        @Override
        public String getId() {
            return TYPE_DAO_ID;
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class TypeRecord implements AttValue {

            private String id;
            private List<EntityRef> parents = new ArrayList<>();
            private EntityRef journal;

            @Override
            public Object getAtt(String name) {
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
