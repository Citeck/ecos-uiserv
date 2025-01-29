package ru.citeck.ecos.uiserv.domain.theme.api.records;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.io.file.EcosFile;
import ru.citeck.ecos.commons.io.file.mem.EcosMemDir;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.commons.utils.ZipUtils;
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
import ru.citeck.ecos.uiserv.domain.theme.dto.ThemeDto;
import ru.citeck.ecos.uiserv.domain.theme.eapps.ThemeArtifactHandler;
import ru.citeck.ecos.uiserv.domain.theme.service.ThemeService;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ThemeRecords extends AbstractRecordsDao implements RecordsQueryDao,
    RecordAttsDao,
    RecordMutateDtoDao<ThemeRecords.ThemeRecord>,
    RecordsDeleteDao {

    private final ThemeService themeService;

    @Nullable
    @Override
    public Object queryRecords(@NotNull RecordsQuery recordsQuery) throws Exception {

        RecsQueryRes<ThemeDto> result = new RecsQueryRes<>();

        if (recordsQuery.getLanguage().equals(PredicateService.LANGUAGE_PREDICATE)) {

            Predicate predicate = recordsQuery.getQuery(Predicate.class);

            List<ThemeDto> themeDtos = themeService.getAll(
                predicate,
                recordsQuery.getPage().getMaxItems(),
                recordsQuery.getPage().getSkipCount(),
                recordsQuery.getSortBy()
            );

            result.setRecords(new ArrayList<>(themeDtos));
            result.setTotalCount(themeService.getCount(predicate));

        } else {
            result.setRecords(new ArrayList<>(
                themeService.getAll(recordsQuery.getPage().getMaxItems(), recordsQuery.getPage().getSkipCount()))
            );
            result.setTotalCount(themeService.getCount());
        }

        return result.withRecords(dto -> new ThemeRecord(dto, themeService));
    }

    @Nullable
    @Override
    public Object getRecordAtts(@NotNull String recordId) throws Exception {
        ThemeDto dto;
        if (recordId.isEmpty()) {
            dto = new ThemeDto();
        } else {
            dto = themeService.getTheme(recordId);
            if (dto == null) {
                dto = new ThemeDto();
            }
        }
        return new ThemeRecord(dto, themeService);
    }

    @NotNull
    @Override
    public List<DelStatus> delete(@NotNull List<String> records) throws Exception {
        records.forEach(themeService::delete);
        return records.stream().map(r -> DelStatus.OK).toList();
    }

    @Override
    public ThemeRecord getRecToMutate(@NotNull String recordId) {
        ThemeDto dto = themeService.getTheme(recordId);
        if (dto == null) {
            dto = new ThemeDto();
            dto.setId(recordId);
        }
        return new ThemeRecord(dto, themeService);
    }

    @NotNull
    @Override
    public String saveMutatedRec(ThemeRecord themeRecord) {
        return themeService.deploy(themeRecord).getId();
    }

    @NotNull
    @Override
    public String getId() {
        return "theme";
    }

    public static class ThemeRecord extends ThemeDto {

        private final ThemeService themeService;

        ThemeRecord(ThemeDto dto, ThemeService themeService) {
            super(dto);
            this.themeService = themeService;
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

        @AttName("isActiveTheme")
        public Boolean getIsActiveTheme() {
            return themeService.getActiveTheme().equals(getId());
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
