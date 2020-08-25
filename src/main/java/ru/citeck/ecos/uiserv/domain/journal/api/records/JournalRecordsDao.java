package ru.citeck.ecos.uiserv.domain.journal.api.records;

import ecos.com.fasterxml.jackson210.annotation.JsonIgnore;
import ecos.com.fasterxml.jackson210.annotation.JsonProperty;
import ecos.com.fasterxml.jackson210.annotation.JsonValue;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.MutableRecordsLocalDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDao;
import ru.citeck.ecos.uiserv.domain.journal.dto.ColumnControl;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalColumnDto;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalDto;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalWithMeta;
import ru.citeck.ecos.uiserv.domain.journal.service.JournalService;
import ru.citeck.ecos.uiserv.domain.journal.service.type.TypeJournalService;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JournalRecordsDao extends LocalRecordsDao
                               implements LocalRecordsQueryWithMetaDao<JournalWithMeta>,
                                          LocalRecordsMetaDao<JournalWithMeta>,
                                          MutableRecordsLocalDao<JournalRecordsDao.JournalRecord> {

    public static final String LANG_QUERY_BY_LIST_ID = "list-id";
    public static final String LANG_QUERY_SITE_JOURNALS = "site-journals";

    private final JournalService journalService;
    private final TypeJournalService typeJournalService;

    @Override
    public RecordsQueryResult<JournalWithMeta> queryLocalRecords(@NotNull RecordsQuery recordsQuery,
                                                                 @NotNull MetaField metaField) {

        RecordsQueryResult<JournalWithMeta> result = new RecordsQueryResult<>();

        if (LANG_QUERY_BY_LIST_ID.equals(recordsQuery.getLanguage())) {

            QueryByListId queryByListId = recordsQuery.getQuery(QueryByListId.class);
            List<JournalWithMeta> journals = journalService.getJournalsByJournalList(queryByListId.getListId());
            journals.forEach(j -> result.addRecord(new JournalRecord(j)));

            return result;

        } else if (LANG_QUERY_SITE_JOURNALS.equals(recordsQuery.getLanguage())) {

            journalService.getJournalsWithSite().forEach(j -> result.addRecord(new JournalRecord(j)));
            return result;
        }

        JournalQueryByTypeRef queryByTypeRef = recordsQuery.getQuery(JournalQueryByTypeRef.class);
        if (queryByTypeRef != null && queryByTypeRef.getTypeRef() != null) {
            typeJournalService.getJournalForType(queryByTypeRef.getTypeRef()).ifPresent(dto ->
                result.addRecord(new JournalRecord(dto))
            );
            return result;
        }

        if (recordsQuery.getLanguage().equals(PredicateService.LANGUAGE_PREDICATE)) {
            Predicate predicate = recordsQuery.getQuery(Predicate.class);

            int max = recordsQuery.getMaxItems();
            if (max <= 0) {
                max = 10000;
            }

            List<JournalWithMeta> journals = journalService.getAll(max, recordsQuery.getSkipCount(), predicate);

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
    public List<JournalWithMeta> getLocalRecordsMeta(@NotNull List<RecordRef> list,
                                                     @NotNull MetaField metaField) {

        return list.stream()
            .map(ref -> {
                JournalWithMeta dto;
                if (RecordRef.isEmpty(ref)) {
                    dto = new JournalWithMeta();
                } else {
                    String id = ref.getId();
                    if (id.startsWith(TypeJournalService.JOURNAL_ID_PREFIX)) {
                        RecordRef typeRef = RecordRef.create(
                            "emodel",
                            "type",
                            id.substring(TypeJournalService.JOURNAL_ID_PREFIX.length())
                        );
                        dto = typeJournalService.getJournalForType(typeRef).orElse(null);
                    } else {
                        dto = journalService.getById(ref.getId());
                    }
                    if (dto == null) {
                        dto = new JournalWithMeta();
                    }
                }
                return new JournalRecord(dto);
            }).collect(Collectors.toList());
    }

    @Override
    public RecordsDelResult delete(RecordsDeletion deletion) {

        List<RecordMeta> resultRecords = new ArrayList<>();

        deletion.getRecords()
            .forEach(r -> {
                journalService.delete(r.getId());
                resultRecords.add(new RecordMeta(r));
            });

        RecordsDelResult result = new RecordsDelResult();
        result.setRecords(resultRecords);
        return result;
    }

    @NotNull
    @Override
    public List<JournalRecord> getValuesToMutate(@NotNull List<RecordRef> records) {

        return records.stream()
            .map(RecordRef::getId)
            .map(id -> {
                JournalWithMeta dto = journalService.getJournalById(id);
                if (dto == null) {
                    dto = new JournalWithMeta();
                    dto.setId(id);
                }
                return new JournalRecord(dto);
            })
            .collect(Collectors.toList());
    }

    @NotNull
    @Override
    public RecordsMutResult save(@NotNull List<JournalRecord> values) {

        RecordsMutResult result = new RecordsMutResult();

        for (final JournalRecord model : values) {
            result.addRecord(new RecordMeta(journalService.save(model).getId()));
        }

        return result;
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

    public static class JournalRecord extends JournalWithMeta {

        JournalRecord(JournalWithMeta dto) {
            super(dto);
        }

        public String getModuleId() {
            return getId();
        }

        public void setModuleId(String value) {
            setId(value);
        }

        @Override
        public void setColumns(List<JournalColumnDto> columns) {
            /*
            // todo
            if (columns != null) {
                columns.forEach(c -> {
                    // convert {"key": "{\"json\":\"value\"}"} to {"key": {"json": "value"}}
                    ColumnControl control = c.getControl();
                    if (control != null) {
                        ObjectData config = control.getConfig();
                        if (config.size() > 0) {
                            ObjectData newConfig = ObjectData.create();
                            for (String key : config.fieldNamesList()) {
                                DataValue value = config.get(key);
                                if (value.isTextual()) {
                                    newConfig.set(key, value.asText());
                                } else {
                                    newConfig.set(key, value);
                                }
                            }
                            control.setConfig(newConfig);
                        }
                    }
                });
            }*/
            super.setColumns(columns);
        }

        @Override
        public List<JournalColumnDto> getColumns() {
            return super.getColumns().stream().map(c -> {
                if (c.getControl() == null) {
                    c = new JournalColumnDto(c);
                    ColumnControl columnControl = new ColumnControl();
                    c.setControl(columnControl);
                }
                if (c.getControl().getConfig() == null) {
                    c.getControl().setConfig(ObjectData.create());
                }
                return c;
            }).collect(Collectors.toList());
        }

        @MetaAtt(".type")
        public RecordRef getEcosType() {
            return RecordRef.valueOf("emodel/type@journal");
        }

        @JsonIgnore
        @MetaAtt(".disp")
        public String getDisplayName() {
            String name = MLText.getClosestValue(getLabel(), QueryContext.getCurrent().getLocale());
            return StringUtils.isNotBlank(name) ? name : "Journal";
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
