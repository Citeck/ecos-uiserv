package ru.citeck.ecos.uiserv.domain.devtools.api.records;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.graphql.meta.value.EmptyValue;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDao;
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDto;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class DevModuleRecords extends LocalRecordsDao implements LocalRecordsQueryWithMetaDao<Object>,
                                                                 LocalRecordsMetaDao<Object> {

    public static final String ID = "dev-module";

    private final Map<String, Record> records = new LinkedHashMap<>();

    public DevModuleRecords() {

        List<Record> modules = new ArrayList<>();

        modules.add(createAlfWebScriptsReloading(
            "alfresco",
            "Alfresco Web Scripts",
            "/alfresco/service/index")
        );
        modules.add(createAlfWebScriptsReloading(
            "share",
            "Share Web Scripts",
            "/share/page/index")
        );

        modules.add(createAlfModule("ecos-apps", "ECOS APPS"));
        modules.add(createAlfModule("journals", "Journals"));
        modules.add(createAlfModule("views", "Views"));
        modules.add(createAlfModule("invariants", "Invariants"));

        modules.forEach(record -> records.put(record.getId(), record));
    }

    @Override
    public RecordsQueryResult<Object> queryLocalRecords(@NotNull RecordsQuery recordsQuery,
                                                        @NotNull MetaField field) {
        return new RecordsQueryResult<>(new ArrayList<>(records.values()));
    }

    @Override
    public List<Object> getLocalRecordsMeta(@NotNull List<RecordRef> records, @NotNull MetaField metaField) {
        return records.stream().map(recRef -> {
            Object rec = this.records.get(recRef.getId());
            if (rec == null) {
                rec = EmptyValue.INSTANCE;
            }
            return rec;
        }).collect(Collectors.toList());
    }

    private Record createAlfWebScriptsReloading(String code, String name, String url) {

        String idPrefix = "alf-webscripts-";

        Record rec = new Record(idPrefix + code);
        rec.setLabel(name);
        rec.setDescription("");

        ActionDto action = new ActionDto();
        action.setId(idPrefix + code + "-reload");
        action.setName(new MLText("Refresh Web Scripts: " + name));
        action.setType("fetch");
        ObjectData config = ObjectData.create();
        config.set("url", url);
        config.set("method", "POST");
        config.set("args", "{\"reset\":\"on\"}");
        action.setConfig(config);
        action.setIcon("icon-reload");

        rec.setActions(Collections.singletonList(action));

        return rec;
    }

    private Record createAlfModule(String code, String name) {

        Record rec = new Record("alf-module-" + code);
        rec.setLabel(name);
        rec.setDescription("");

        ActionDto action = new ActionDto();
        action.setId("dev-module-" + code + "-reload");
        action.setName(new MLText("Reload " + name));
        action.setType("fetch");
        ObjectData config = ObjectData.create();
        config.set("url", "/share/proxy/alfresco/citeck/dev/reset-cache");
        config.set("method", "POST");
        config.set("body", "{\"" + code + "\":true}");
        action.setConfig(config);
        action.setIcon("icon-reload");

        rec.setActions(Collections.singletonList(action));

        return rec;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Data
    @RequiredArgsConstructor
    public static class Record {

        private String label;
        private String description;

        private final String id;

        @MetaAtt("_actions")
        private List<ActionDto> actions;

        public String getModuleId() {
            return id;
        }

        @MetaAtt(".type")
        public RecordRef getEcosType() {
            return RecordRef.valueOf("emodel/type@dev-module");
        }
    }
}

