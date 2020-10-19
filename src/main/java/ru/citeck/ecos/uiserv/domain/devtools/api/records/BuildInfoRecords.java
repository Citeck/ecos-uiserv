package ru.citeck.ecos.uiserv.domain.devtools.api.records;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.EmptyValue;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDao;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class BuildInfoRecords extends LocalRecordsDao implements LocalRecordsQueryWithMetaDao<Object>,
                                                                 LocalRecordsMetaDao<Object> {

    public static final String ID = "build-info";

    private final Record uiservBuildInfo;

    public BuildInfoRecords() {

        ObjectData buildInfo = null;

        try {
            File buildInfoFile = ResourceUtils.getFile("classpath:build-info/full.json");
            if (buildInfoFile.exists()) {
                buildInfo = Json.getMapper().read(buildInfoFile, ObjectData.class);
            }
        } catch (FileNotFoundException e) {
            // do nothing
        } catch (Exception e) {
            log.error("Build info reading failed", e);
        }

        if (buildInfo == null) {
            buildInfo = ObjectData.create();
            buildInfo.set("buildDate", new Date().toInstant().toString());
            buildInfo.set("commits", "[]");
        }
        uiservBuildInfo = new Record("uiserv", "UI Server", "", buildInfo);
    }

    @Override
    public RecordsQueryResult<Object> queryLocalRecords(@NotNull RecordsQuery recordsQuery,
                                                        @NotNull MetaField field) {

        return new RecordsQueryResult<>(Collections.singletonList(uiservBuildInfo));
    }

    @Override
    public List<Object> getLocalRecordsMeta(@NotNull List<RecordRef> records, @NotNull MetaField metaField) {

        return records.stream().map(ref -> {
            if (ref.getId().equals("uiserv")) {
                return uiservBuildInfo;
            }
            return EmptyValue.INSTANCE;
        }).collect(Collectors.toList());
    }

    @Override
    public String getId() {
        return ID;
    }

    @Data
    @RequiredArgsConstructor
    public static class Record {
        private final String id;
        private final String label;
        private final String description;
        private final ObjectData info;
    }
}

