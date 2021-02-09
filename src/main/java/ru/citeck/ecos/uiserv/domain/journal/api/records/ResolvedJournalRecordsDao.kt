package ru.citeck.ecos.uiserv.domain.journal.api.records;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordConstants;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.predicate.PredicateUtils;
import ru.citeck.ecos.records2.predicate.model.AndPredicate;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.predicate.model.ValuePredicate;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDao;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalColumnDto;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalWithMeta;
import ru.citeck.ecos.uiserv.domain.journal.dto.ResolvedJournalDto;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ResolvedJournalRecordsDao extends LocalRecordsDao
                               implements LocalRecordsQueryWithMetaDao<ResolvedJournalDto>,
                                          LocalRecordsMetaDao<ResolvedJournalDto> {

    private final JournalRecordsDao journalRecordsDao;

    {
        setId("resolved-journal");
    }

    @Override
    public RecordsQueryResult<ResolvedJournalDto> queryLocalRecords(@NotNull RecordsQuery recordsQuery,
                                                                    @NotNull MetaField metaField) {

        RecordsQueryResult<JournalWithMeta> records = journalRecordsDao.queryLocalRecords(recordsQuery, metaField);
        return new RecordsQueryResult<>(records, this::resolveJournal);
    }

    @Override
    public List<ResolvedJournalDto> getLocalRecordsMeta(@NotNull List<RecordRef> list,
                                                        @NotNull MetaField metaField) {

        return resolveJournals(journalRecordsDao.getLocalRecordsMeta(list, metaField));
    }

    public List<ResolvedJournalDto> resolveJournals(List<JournalWithMeta> journals) {
        return journals.stream().map(this::resolveJournal).collect(Collectors.toList());
    }

    public ResolvedJournalDto resolveJournal(JournalWithMeta journal) {

        ResolvedJournalDto resolvedDto = new ResolvedJournalDto(journal);

        if (StringUtils.isBlank(journal.getId())) {
            return resolvedDto;
        }

        resolveEdgeMeta(resolvedDto);
        resolvePredicate(resolvedDto);

        return resolvedDto;
    }

    private void resolveEdgeMeta(ResolvedJournalDto journalDto) {

        if (RecordRef.isEmpty(journalDto.getMetaRecord())) {
            return;
        }

        Map<String, String> attributeEdges = new HashMap<>();
        Map<String, JournalColumnDto> columnByName = new HashMap<>();

        for (JournalColumnDto columnDto : journalDto.getColumns()) {

            columnByName.put(columnDto.getName(), columnDto);
            String attribute = columnDto.getAttribute();
            if (StringUtils.isBlank(attribute)) {
                attribute = columnDto.getName();
            }


            if (!attribute.contains(".")) {

                List<String> edgeAtts = new ArrayList<>();

                if (StringUtils.isBlank(columnDto.getType())) {
                    edgeAtts.add("type");
                }
                if (columnDto.getEditable() == null) {
                    edgeAtts.add("protected");
                }
                if (MLText.isEmpty(columnDto.getLabel())) {
                    edgeAtts.add("title");
                }
                if (columnDto.getMultiple() == null) {
                    edgeAtts.add("multiple");
                }

                if (!edgeAtts.isEmpty()) {
                    if (edgeAtts.size() == 1) {
                        edgeAtts.add("javaClass"); // protection from optimization
                    }
                    attributeEdges.put(columnDto.getName(),
                        ".edge(n:\"" + attribute + "\"){" + String.join(",", edgeAtts) + "}");
                }
            }
        }

        if (!attributeEdges.isEmpty()) {

            RecordMeta attributes = recordsService.getAttributes(journalDto.getMetaRecord(), attributeEdges);

            attributes.forEachJ((name, value) -> {
                JournalColumnDto dto = columnByName.get(name);
                if (dto != null) {
                    dto.setType(value.get("type").asText());
                    if (StringUtils.isBlank(dto.getType())) {
                        dto.setType("text");
                    }
                    if (value.has("protected")) {
                        dto.setEditable(!value.get("protected").asBoolean());
                    }
                    if (value.has("title")) {
                        dto.setLabel(new MLText(value.get("title").asText()));
                    }
                    if (value.has("multiple")) {
                        dto.setMultiple(value.get("multiple").asBoolean());
                    }
                }
            });
        }
    }

    private void resolvePredicate(ResolvedJournalDto journalDto) {

        if (RecordRef.isEmpty(journalDto.getTypeRef())) {
            return;
        }
        Predicate fullPredicate;
        Predicate typePredicate = ValuePredicate.eq("_type", journalDto.getTypeRef().toString());
        if (journalDto.getPredicate() != null) {
            Predicate basePredicate = journalDto.getPredicate().getAs(Predicate.class);
            List<String> atts = PredicateUtils.getAllPredicateAttributes(basePredicate);
            if (atts.contains(RecordConstants.ATT_ECOS_TYPE) || atts.contains(RecordConstants.ATT_TYPE)) {
                fullPredicate = basePredicate;
            } else {
                fullPredicate = AndPredicate.of(basePredicate, typePredicate);
            }
        } else {
            fullPredicate = typePredicate;
        }
        journalDto.setPredicate(Json.getMapper().convert(fullPredicate, ObjectData.class));
    }
}
