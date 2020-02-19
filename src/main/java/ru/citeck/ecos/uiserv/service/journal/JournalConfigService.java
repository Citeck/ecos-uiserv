package ru.citeck.ecos.uiserv.service.journal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.objdata.DataValue;
import ru.citeck.ecos.records2.utils.json.JsonUtils;
import ru.citeck.ecos.uiserv.domain.File;
import ru.citeck.ecos.uiserv.domain.FileType;
import ru.citeck.ecos.uiserv.service.file.FileService;
import ru.citeck.ecos.uiserv.service.file.FileViewCaching;

import java.beans.FeatureDescriptor;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class JournalConfigService {
    private final FileViewCaching<JournalConfigDownstream> caching;
    private final Mapper mapper;

    public JournalConfigService(FileService fileService, @Lazy Mapper mapper) {
        this.mapper = mapper;
        this.caching = new FileViewCaching<>(
            key -> fileService.loadFile(FileType.JOURNALCFG, key),
            mapper::unmarshalFile);
    }

    public Optional<JournalConfigDownstream> getJournalConfig(String journalId) {
        return caching.get(journalId);
    }

    @Bean
    public FileService.FileMetadataExtractorInfo journalConfigFileMetadataExtractor() {
        return new FileService.FileMetadataExtractorInfo(FileType.JOURNALCFG,
            bytes -> mapper.unmarshal(bytes, true).getId());
    }

    @Configuration
    @RequiredArgsConstructor
    public static class Mapper {
        private final ObjectMapper objectMapper;
        private final RecordsService recordsService;


        public Optional<JournalConfigDownstream> unmarshalFile(File file) {
            return Optional.of(unmarshal(file.getFileVersion().getBytes(), false));
        }

        //need onlyId because some other parts involve querying other services
        private JournalConfigDownstream unmarshal(byte[] json, boolean onlyId) {
            final JournalConfigDB db;
            //When reading older json versions, we can read those into same class but with custom
            //deserializers or field aliases, then write that one to String and then finally read
            //up-to-date JournalClass from that String.
            try {
                db = objectMapper.readValue(json, JournalConfigDB.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            final JournalConfigDownstream result = new JournalConfigDownstream();
            result.setId(db.getId());
            if (onlyId)
                return result;

            final Map<String, AttInfo> attributesInfo = getAttributesInfo(db.getSourceId(),
                db.getColumns().stream().map(ColumnDB::getAttribute).collect(Collectors.toList()),
                AttInfo.class);

            result.setColumns(db.getColumns().stream()
                .map(c -> {
                    final Column mapped = new Column();
                    mapped.setText(c.getText());

                    final AttInfo info = attributesInfo.get(c.getAttribute());
                    if (info != null) {
                        mapped.setEditorKey(info.getEditorKey());
                        mapped.setType(info.getType());

                        mapped.setAttribute(c.getAttribute());
                        mapped.setFormatter(c.getFormatter());
                        mapped.setParams(c.getParams());
                        mapped.setDefault(c.isDefault());
                        mapped.setSearchable(c.isSearchable());
                        mapped.setSortable(c.isSortable());
                        mapped.setVisible(c.isVisible());
                        mapped.setGroupable(c.isGroupable());
                    }
                    return mapped;
                })
                .collect(Collectors.toList()));
            result.setMeta(new JournalMeta());
            result.getMeta().setPredicate(db.getPredicate());
            result.getMeta().setCreateVariants(db.getCreateVariants());
            result.setParams(db.getParams());
            result.setSourceId(db.getSourceId());
            return result;
        }

        private <T> Map<String, T> getAttributesInfo(String sourceId, List<String> attributes,
                                                     Class<T> attInfoClass) {
            //todo RecordsMetaService has its own way to do that, but this will have to wait
            final List<String> propNames = Stream.of(BeanUtils.getPropertyDescriptors(attInfoClass))
                .filter(x -> !x.getName().equals("class")) //todo wrong! But doing this right is way too difficult
                .map(FeatureDescriptor::getName).collect(Collectors.toList());

            Map<String, String> attributesEdges = new HashMap<>();
            final String nameset = String.join(",", propNames);
            for (String attribute : attributes) {
                attributesEdges.put(attribute, ".edge(n:\"" + attribute + "\"){" + nameset + "}");
            }

            final RecordRef recordRef = RecordRef.valueOf("alfresco/@" + sourceId + "@");
            RecordMeta attInfoMeta = recordsService.getAttributes(recordRef, attributesEdges);

            Map<String, T> result = new HashMap<>();

            for (String attribute : attributes) {

                T info = null;

                DataValue attInfoNode = attInfoMeta.get(attribute);
                if (attInfoNode.isObject()) {
                    info = JsonUtils.convert(attInfoNode, attInfoClass);
                }

                result.put(attribute, info);
            }

            return result;
        }
    }

    @Getter
    @Setter
    public static class JournalConfigDownstream {
        String id;
        String sourceId;
        JournalMeta meta;
        List<Column> columns;
        Map<String, String> params;
    }

    @Getter
    @Setter
    public static class JournalConfigDB {
        String id;
        String sourceId;
        JsonNode predicate;
        List<ResponseVariant> createVariants;
        List<ColumnDB> columns;
        Map<String, String> params;
    }

    @Getter
    @Setter
    static class JournalMeta {
        String nodeRef;
        List<Criterion> criteria;
        JsonNode predicate;
        List<ResponseVariant> createVariants;
    }

    @Getter
    @Setter
    static class Criterion {
        String field;
        String predicate;
        String value;
    }

    @Getter
    @Setter
    static class Column {
        String text;
        String type;
        String editorKey;
        String attribute;
        Formatter formatter;
        Map<String, String> params;
        boolean isDefault;
        boolean isSearchable;
        boolean isSortable;
        boolean isVisible;
        boolean isGroupable;
    }

    @Getter
    @Setter
    static class ColumnDB {
        String text;
        String attribute;
        Formatter formatter;
        Map<String, String> params;
        boolean isDefault;
        boolean isSearchable;
        boolean isSortable;
        boolean isVisible;
        boolean isGroupable;
    }

    @Getter
    @Setter
    static class Formatter {
        String name;
        Map<String, String> params;
    }

    @Getter
    @Setter
    static class AttInfo {
        String type;
        String editorKey;
    }

    @Getter
    @Setter
    public static class ResponseVariant {
        String title;
        String destination;
        String type;
        String formId;
        boolean isDefault;
        boolean canCreate;
        String createArguments;
    }
}
