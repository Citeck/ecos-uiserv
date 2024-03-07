package ru.citeck.ecos.uiserv.domain.theme.api.records;

import ecos.com.fasterxml.jackson210.annotation.JsonIgnore;
import ecos.com.fasterxml.jackson210.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.io.file.EcosFile;
import ru.citeck.ecos.commons.io.file.mem.EcosMemDir;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.ZipUtils;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
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
import ru.citeck.ecos.uiserv.domain.theme.dto.ThemeDto;
import ru.citeck.ecos.uiserv.domain.theme.eapps.ThemeArtifactHandler;
import ru.citeck.ecos.uiserv.domain.theme.service.ThemeService;
import ru.citeck.ecos.uiserv.domain.utils.LegacyRecordsUtils;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ThemeRecords extends LocalRecordsDao implements LocalRecordsQueryWithMetaDao<ThemeRecords.ThemeRecord>,
    LocalRecordsMetaDao<ThemeRecords.ThemeRecord>,
    MutableRecordsLocalDao<ThemeRecords.ThemeRecord> {

    private final ThemeService themeService;

    @Override
    public RecordsQueryResult<ThemeRecord> queryLocalRecords(@NotNull RecordsQuery recordsQuery,
                                                             @NotNull MetaField metaField) {

        RecordsQueryResult<ThemeDto> result = new RecordsQueryResult<>();

        if (recordsQuery.getLanguage().equals(PredicateService.LANGUAGE_PREDICATE)) {

            Predicate predicate = recordsQuery.getQuery(Predicate.class);

            List<ThemeDto> themeDtos = themeService.getAll(
                predicate,
                recordsQuery.getMaxItems(),
                recordsQuery.getSkipCount(),
                LegacyRecordsUtils.mapLegacySortBy(recordsQuery.getSortBy())
            );

            result.setRecords(new ArrayList<>(themeDtos));
            result.setTotalCount(themeService.getCount(predicate));

        } else {
            result.setRecords(new ArrayList<>(
                themeService.getAll(recordsQuery.getMaxItems(), recordsQuery.getSkipCount()))
            );
            result.setTotalCount(themeService.getCount());
        }

        return new RecordsQueryResult<>(result, ThemeRecord::new);
    }

    @Override
    public List<ThemeRecord> getLocalRecordsMeta(@NotNull List<EntityRef> list, @NotNull MetaField metaField) {

        return list.stream()
            .map(ref -> {
                ThemeDto dto;
                if (EntityRef.isEmpty(ref)) {
                    dto = new ThemeDto();
                } else {
                    dto = themeService.getTheme(ref.getLocalId());
                    if (dto == null) {
                        dto = new ThemeDto();
                    }
                }
                return new ThemeRecord(dto);
            }).collect(Collectors.toList());
    }

    @Override
    public RecordsDelResult delete(RecordsDeletion deletion) {

        List<RecordMeta> resultRecords = new ArrayList<>();

        deletion.getRecords()
            .forEach(r -> {
                themeService.delete(r.getLocalId());
                resultRecords.add(new RecordMeta(r));
            });

        RecordsDelResult result = new RecordsDelResult();
        result.setRecords(resultRecords);
        return result;
    }

    @NotNull
    @Override
    public List<ThemeRecord> getValuesToMutate(@NotNull List<EntityRef> records) {

        return records.stream()
            .map(EntityRef::getLocalId)
            .map(id -> {
                ThemeDto dto = themeService.getTheme(id);
                if (dto == null) {
                    dto = new ThemeDto();
                    dto.setId(id);
                }
                return new ThemeRecord(dto);
            })
            .collect(Collectors.toList());
    }

    @NotNull
    @Override
    public RecordsMutResult save(@NotNull List<ThemeRecord> values) {

        RecordsMutResult result = new RecordsMutResult();

        for (final ThemeRecord model : values) {
            result.addRecord(new RecordMeta(themeService.deploy(model).getId()));
        }

        return result;
    }

    @Override
    public String getId() {
        return "theme";
    }

    public static class ThemeRecord extends ThemeDto {

        ThemeRecord(ThemeDto dto) {
            super(dto);
        }

        public String getModuleId() {
            return getId();
        }

        public void setModuleId(String value) {
            setId(value);
        }

        public String getEcosType() {
            return "theme";
        }

        @JsonIgnore
        @AttName("?disp")
        public String getDisplayName() {
            return super.getId();
        }

        @JsonProperty("_content")
        public void setContent(List<ObjectData> content) {

            String base64Content = content.get(0).get("url", "");

            Pattern pattern = Pattern.compile("^data:(.+?);base64,(.+)$");
            Matcher matcher = pattern.matcher(base64Content);
            if (!matcher.find()) {
                throw new IllegalStateException("Incorrect data: " + base64Content);
            }

            //String mimetype = matcher.group(1);
            String base64 = matcher.group(2);

            byte[] bytes = Base64.getDecoder().decode(base64);
            EcosMemDir ecosMemDir = ZipUtils.extractZip(bytes);

            EcosFile theme = ecosMemDir.getChildren().get(0);
            this.setModuleId(theme.getName());

            ThemeArtifactHandler.DeployModuleMeta meta = Json.getMapper().read(
                theme.getFile("meta.json"),
                ThemeArtifactHandler.DeployModuleMeta.class);

            if (meta == null) {
                throw new IllegalStateException("meta.json is not found");
            }

            this.setImages(meta.getImages());
            this.setName(meta.getName());

            Map<String, byte[]> resources = new HashMap<>();
            for (EcosFile file : theme.findFiles()) {
                String name = file.getName();
                if (ThemeService.RES_EXTENSIONS.stream().anyMatch(name::endsWith)) {
                    String path = "/" + theme.getPath().relativize(file.getPath()).toString();
                    resources.put(path, file.readAsBytes());
                }
            }
            this.setResources(resources);
        }

        public byte[] getData() {

            EcosMemDir root = new EcosMemDir();
            EcosFile dir = root.createDir(getId());

            ThemeArtifactHandler.DeployModuleMeta meta = new ThemeArtifactHandler.DeployModuleMeta();

            meta.setImages(getImages());
            meta.setName(getName());

            byte[] metaData = Json.getMapper().toBytes(meta);
            if (metaData == null) {
                throw new IllegalStateException("Mapping error. Meta: " + meta);
            }
            dir.createFile("meta.json", metaData);

            if (getResources() != null) {
                for (Map.Entry<String, byte[]> entry : getResources().entrySet()) {
                    dir.createFile(entry.getKey().substring(1), entry.getValue());
                }
            }
            return ZipUtils.writeZipAsBytes(root);
        }
    }
}
