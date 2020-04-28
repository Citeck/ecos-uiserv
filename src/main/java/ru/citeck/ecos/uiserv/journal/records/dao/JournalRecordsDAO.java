package ru.citeck.ecos.uiserv.journal.records.dao;

import ecos.com.fasterxml.jackson210.annotation.JsonIgnore;
import ecos.com.fasterxml.jackson210.annotation.JsonProperty;
import ecos.com.fasterxml.jackson210.annotation.JsonValue;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.annotation.DisplayName;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDAO;
import ru.citeck.ecos.uiserv.journal.dto.JournalDto;
import ru.citeck.ecos.uiserv.journal.service.JournalService;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JournalRecordsDAO extends LocalRecordsDAO
    implements LocalRecordsQueryWithMetaDAO<JournalDto>,
               LocalRecordsMetaDAO<JournalDto> {

    public static final String LANG_QUERY_BY_LIST_ID = "list-id";

    private final JournalService journalService;

    @Override
    public RecordsQueryResult<JournalDto> queryLocalRecords(RecordsQuery recordsQuery, MetaField metaField) {

        RecordsQueryResult<JournalDto> result = new RecordsQueryResult<>();

        if (LANG_QUERY_BY_LIST_ID.equals(recordsQuery.getLanguage())) {

            QueryByListId queryByListId = recordsQuery.getQuery(QueryByListId.class);
            List<JournalDto> journals = journalService.getJournalsByJournalsList(queryByListId.listId);

            journals.forEach(j -> result.addRecord(new JournalRecord(j)));

            return result;
        }

        JournalQueryByTypeRef queryByTypeRef = recordsQuery.getQuery(JournalQueryByTypeRef.class);
        if (queryByTypeRef != null && queryByTypeRef.getTypeRef() != null) {
            JournalDto dto = journalService.searchJournalByTypeRef(queryByTypeRef.getTypeRef());
            if (dto != null) {
                result.addRecord(new JournalRecord(dto));
            }
            return result;
        }

        if (recordsQuery.getLanguage().equals(PredicateService.LANGUAGE_PREDICATE)) {
            Predicate predicate = recordsQuery.getQuery(Predicate.class);

            int max = recordsQuery.getMaxItems();
            if (max <= 0) {
                max = 10000;
            }

            Set<JournalDto> journals = journalService.getAll(max, recordsQuery.getSkipCount(), predicate);

            result.setRecords(new ArrayList<>(journals));
            result.setTotalCount(journalService.getCount(predicate));

        } else {
            result.setRecords(new ArrayList<>(
                journalService.getAll(recordsQuery.getMaxItems(), recordsQuery.getSkipCount()))
            );
            result.setTotalCount(journalService.getCount());
        }

        return new RecordsQueryResult<>(result, JournalRecord::new);
    }

    @Override
    public List<JournalDto> getLocalRecordsMeta(List<RecordRef> list, MetaField metaField) {

        return list.stream()
            .map(ref -> {
                JournalDto dto;
                if (RecordRef.isEmpty(ref)) {
                    dto = new JournalDto();
                } else {
                    dto = journalService.getById(ref.getId());
                    if (dto == null) {
                        dto = new JournalDto();
                    }
                }
                return new JournalRecord(dto);
            }).collect(Collectors.toList());
    }

    @Data
    public static class QueryWithTypeRef {
        private String typeRef;
    }

    @Data
    public static class QueryByListId {
        private String listId;
    }

    @Override
    public String getId() {
        return "journal";
    }

    @Data
    public static class JournalQueryByTypeRef {
        private RecordRef typeRef;
    }

    public static class JournalRecord extends JournalDto {

        JournalRecord() {
        }

        JournalRecord(JournalDto dto) {
            super(dto);
        }

        public String getModuleId() {
            return getId();
        }

        public void setModuleId(String value) {
            setId(value);
        }

        @JsonIgnore
        @DisplayName
        public String getDisplayName() {
            String result = getId();
            return result != null ? result : "Journal";
        }

        @JsonProperty("_content")
        public void setContent(List<ObjectData> content) {

            String base64Content = content.get(0).get("url", "");
            base64Content = base64Content.replaceAll("^data:application/json;base64,", "");
            ObjectData data = Json.getMapper().read(Base64.getDecoder().decode(base64Content), ObjectData.class);

            Json.getMapper().applyData(this, data);
        }

        @JsonValue
        @com.fasterxml.jackson.annotation.JsonValue
        public JournalDto toJson() {
            return new JournalDto(this);
        }
    }
}
