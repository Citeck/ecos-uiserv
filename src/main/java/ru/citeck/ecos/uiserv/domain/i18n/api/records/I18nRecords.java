package ru.citeck.ecos.uiserv.domain.i18n.api.records;

import ecos.com.fasterxml.jackson210.annotation.JsonIgnore;
import ecos.com.fasterxml.jackson210.annotation.JsonProperty;
import ecos.com.fasterxml.jackson210.annotation.JsonValue;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.json.YamlUtils;
import ru.citeck.ecos.events2.type.RecordEventsService;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
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
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.uiserv.domain.i18n.dto.I18nDto;
import ru.citeck.ecos.uiserv.domain.i18n.service.I18nService;
import ru.citeck.ecos.uiserv.domain.utils.LegacyRecordsUtils;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class I18nRecords extends LocalRecordsDao implements LocalRecordsQueryWithMetaDao<I18nRecords.I18nRecord>,
    LocalRecordsMetaDao<I18nRecords.I18nRecord>,
    MutableRecordsLocalDao<I18nRecords.I18nRecord> {

    private final I18nService i18nService;
    private final RecordEventsService recordEventsService;

    @PostConstruct
    public void init() {
        i18nService.addListener((before, after) ->
            recordEventsService.emitRecChanged(before, after, getId(), I18nRecord::new)
        );
    }

    @Override
    public RecordsQueryResult<I18nRecord> queryLocalRecords(@NotNull RecordsQuery recordsQuery,
                                                                 @NotNull MetaField metaField) {

        RecordsQueryResult<I18nDto> result = new RecordsQueryResult<>();

        if (recordsQuery.getLanguage().equals(PredicateService.LANGUAGE_PREDICATE)) {

            Predicate predicate = recordsQuery.getQuery(Predicate.class);

            List<I18nDto> i18nDtos = i18nService.getAll(
                predicate,
                recordsQuery.getMaxItems(),
                recordsQuery.getSkipCount(),
                LegacyRecordsUtils.mapLegacySortBy(recordsQuery.getSortBy())
            );

            result.setRecords(new ArrayList<>(i18nDtos));
            result.setTotalCount(i18nService.getCount(predicate));

        } else {
            result.setRecords(new ArrayList<>(
                i18nService.getAll(recordsQuery.getMaxItems(), recordsQuery.getSkipCount()))
            );
            result.setTotalCount(i18nService.getCount());
        }

        return new RecordsQueryResult<>(result, I18nRecord::new);
    }

    @Override
    public List<I18nRecord> getLocalRecordsMeta(@NotNull List<RecordRef> list,
                                                     @NotNull MetaField metaField) {

        return list.stream()
            .map(ref -> {
                I18nDto dto;
                if (RecordRef.isEmpty(ref)) {
                    dto = new I18nDto();
                } else {
                    dto = i18nService.getById(ref.getId());
                    if (dto == null) {
                        dto = new I18nDto();
                    }
                }
                return new I18nRecord(dto);
            }).collect(Collectors.toList());
    }

    @Override
    public RecordsDelResult delete(RecordsDeletion deletion) {

        List<RecordMeta> resultRecords = new ArrayList<>();

        deletion.getRecords()
            .forEach(r -> {
                i18nService.delete(r.getId());
                resultRecords.add(new RecordMeta(r));
            });

        RecordsDelResult result = new RecordsDelResult();
        result.setRecords(resultRecords);
        return result;
    }

    @NotNull
    @Override
    public List<I18nRecord> getValuesToMutate(@NotNull List<RecordRef> records) {

        return records.stream()
            .map(RecordRef::getId)
            .map(id -> {
                I18nDto dto = i18nService.getById(id);
                if (dto == null) {
                    dto = new I18nDto();
                    dto.setId(id);
                }
                return new I18nRecord(dto);
            })
            .collect(Collectors.toList());
    }

    @NotNull
    @Override
    public RecordsMutResult save(@NotNull List<I18nRecord> values) {

        RecordsMutResult result = new RecordsMutResult();

        for (final I18nRecord model : values) {
            result.addRecord(new RecordMeta(i18nService.upload(model).getId()));
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
        return "i18n";
    }

    public static class I18nRecord extends I18nDto {

        I18nRecord(I18nDto dto) {
            super(dto);
        }

        public String getModuleId() {
            return getId();
        }

        public void setModuleId(String value) {
            setId(value);
        }

        public String getEcosType() {
            return "i18n";
        }

        @JsonIgnore
        @AttName(".disp")
        public String getDisplayName() {
            return super.getId();
        }

        @JsonProperty("_content")
        public void setContent(List<ObjectData> content) {

            String dataUriContent = content.get(0).get("url", "");
            ObjectData data = Json.getMapper().read(dataUriContent, ObjectData.class);

            Json.getMapper().applyData(this, data);
        }

        @JsonValue
        @com.fasterxml.jackson.annotation.JsonValue
        public I18nDto toJson() {
            return new I18nDto(this);
        }

        public byte[] getData() {
            return YamlUtils.toNonDefaultString(toJson()).getBytes(StandardCharsets.UTF_8);
        }
    }
}
