package ru.citeck.ecos.uiserv.domain.i18n.api.records;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.json.YamlUtils;
import ru.citeck.ecos.events2.type.RecordEventsService;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao;
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao;
import ru.citeck.ecos.records3.record.dao.delete.DelStatus;
import ru.citeck.ecos.records3.record.dao.delete.RecordsDeleteDao;
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDtoDao;
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao;
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery;
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes;
import ru.citeck.ecos.uiserv.app.common.perms.UiServSystemArtifactPerms;
import ru.citeck.ecos.uiserv.domain.i18n.dto.I18nDto;
import ru.citeck.ecos.uiserv.domain.i18n.service.I18nService;

import jakarta.annotation.PostConstruct;
import ru.citeck.ecos.webapp.api.constants.AppName;
import ru.citeck.ecos.webapp.api.entity.EntityRef;
import ru.citeck.ecos.webapp.lib.perms.RecordPerms;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class I18nRecords extends AbstractRecordsDao implements RecordsQueryDao,
    RecordAttsDao,
    RecordMutateDtoDao<I18nRecords.I18nRecord>,
    RecordsDeleteDao {

    public static final String ID = "i18n";

    private final I18nService i18nService;
    private final RecordEventsService recordEventsService;
    private final UiServSystemArtifactPerms perms;

    @PostConstruct
    public void init() {
        i18nService.addListener((before, after) ->
            recordEventsService.emitRecChanged(before, after, getId(), I18nRecord::new)
        );
    }

    @Nullable
    @Override
    public Object queryRecords(@NotNull RecordsQuery recordsQuery) throws Exception {

        RecsQueryRes<I18nDto> result = new RecsQueryRes<>();

        if (recordsQuery.getLanguage().equals(PredicateService.LANGUAGE_PREDICATE)) {

            Predicate predicate = recordsQuery.getQuery(Predicate.class);

            List<I18nDto> i18nDtos = i18nService.getAll(
                predicate,
                recordsQuery.getPage().getMaxItems(),
                recordsQuery.getPage().getSkipCount(),
                recordsQuery.getSortBy()
            );

            result.setRecords(new ArrayList<>(i18nDtos));
            result.setTotalCount(i18nService.getCount(predicate));

        } else {
            result.setRecords(new ArrayList<>(
                i18nService.getAll(recordsQuery.getPage().getMaxItems(), recordsQuery.getPage().getSkipCount()))
            );
            result.setTotalCount(i18nService.getCount());
        }

        return result.withRecords(I18nRecord::new);
    }

    @Nullable
    @Override
    public Object getRecordAtts(@NotNull String recordId) throws Exception {
        I18nDto dto;
        if (recordId.isEmpty()) {
            dto = new I18nDto();
        } else {
            dto = i18nService.getById(recordId);
            if (dto == null) {
                dto = new I18nDto();
            }
        }
        return new I18nRecord(dto);
    }

    @NotNull
    @Override
    public List<DelStatus> delete(@NotNull List<String> records) throws Exception {
        records.forEach(i18nService::delete);
        return records.stream().map(r -> DelStatus.OK).toList();
    }

    @Override
    public I18nRecord getRecToMutate(@NotNull String recordId) {
        I18nDto dto = i18nService.getById(recordId);
        if (dto == null) {
            dto = new I18nDto();
            dto.setId(recordId);
        }
        return new I18nRecord(dto);
    }

    @NotNull
    @Override
    public String saveMutatedRec(I18nRecord i18nRecord) {
        return i18nService.upload(i18nRecord).getId();
    }


    @Data
    public static class QueryWithTypeRef {
        private String typeRef;
    }

    @Data
    public static class QueryByListId {
        private String listId;
    }

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    public class I18nRecord extends I18nDto {

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
        @AttName("?disp")
        public String getDisplayName() {
            return super.getId();
        }

        @JsonProperty("_content")
        public void setContent(List<ObjectData> content) {

            String dataUriContent = content.getFirst().get("url", "");
            ObjectData data = Json.getMapper().read(dataUriContent, ObjectData.class);

            Json.getMapper().applyData(this, data);
        }

        @JsonValue
        public I18nDto toJson() {
            return new I18nDto(this);
        }

        public byte[] getData() {
            return YamlUtils.toNonDefaultString(toJson()).getBytes(StandardCharsets.UTF_8);
        }

        public RecordPerms getPermissions() {
            return perms.getPerms(EntityRef.create(AppName.UISERV, ID, getId()));
        }
    }
}
