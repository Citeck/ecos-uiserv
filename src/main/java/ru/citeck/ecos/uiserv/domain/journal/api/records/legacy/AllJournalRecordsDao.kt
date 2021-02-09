package ru.citeck.ecos.uiserv.domain.journal.api.records;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.request.error.ErrorUtils;
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.predicate.PredicateUtils;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records2.request.query.QueryConsistency;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDao;
import ru.citeck.ecos.records3.record.request.RequestContext;
import ru.citeck.ecos.records3.record.request.msg.MsgLevel;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalWithMeta;
import ru.citeck.ecos.uiserv.domain.journal.dto.legacy1.JournalConfigResp;
import ru.citeck.ecos.uiserv.domain.journal.service.JournalServiceImpl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AllJournalRecordsDao extends LocalRecordsDao
    implements LocalRecordsQueryWithMetaDao<JournalWithMeta>,
               LocalRecordsMetaDao<JournalWithMeta> {

    private final JournalRecordsDao journalRecordsDao;
    private final JournalV1RecordsDao journalV1RecordsDao;

    @Override
    public List<JournalWithMeta> getLocalRecordsMeta(@NotNull List<RecordRef> records, @NotNull MetaField metaField) {
        return records.stream()
            .map(r -> {
                try {
                    return getJournalMeta(r, metaField);
                } catch (Exception e) {
                    RequestContext.getCurrentNotNull().addMsg(MsgLevel.ERROR, () -> ErrorUtils.convertException(e));
                    log.error("Journal resolving error", e);
                    return new JournalWithMeta();
                }
            })
            .map(JournalRecordsDao.JournalRecord::new)
            .collect(Collectors.toList());
    }

    private JournalWithMeta getJournalMeta(RecordRef recordRef, MetaField metaField) {

        List<RecordRef> refsList = Collections.singletonList(RecordRef.valueOf(recordRef.getId()));
        List<JournalWithMeta> meta = journalRecordsDao.getLocalRecordsMeta(refsList, metaField);
        if (meta.isEmpty() || StringUtils.isBlank(meta.get(0).getId())) {
            List<JournalConfigResp> metaV1List = journalV1RecordsDao.getLocalRecordsMeta(refsList, metaField);
            if (metaV1List.isEmpty() || StringUtils.isBlank(metaV1List.get(0).getId())) {
                return new JournalWithMeta();
            }
            JournalWithMeta result = new JournalWithMeta();
            JournalConfigResp metaV1 = metaV1List.get(0);

            result.setLabel(new MLText(metaV1.getDisplayName()));
            result.setId(recordRef.getId());
            return result;
        }
        return meta.get(0);
    }

    @Override
    public RecordsQueryResult<JournalWithMeta> queryLocalRecords(@NotNull RecordsQuery recordsQuery,
                                                                 @NotNull MetaField metaField) {

        RecordsQueryResult<JournalWithMeta> result = journalRecordsDao.queryLocalRecords(recordsQuery, metaField);

        int found = result.getRecords().size();
        if (found >= recordsQuery.getMaxItems()) {
            if ((recordsQuery.getSkipCount() + found) == result.getTotalCount()) {
                result.setTotalCount(result.getRecords().size() + 1);
            }
            return result;
        }

        Predicate predicate = recordsQuery.getQuery(Predicate.class);
        JournalServiceImpl.PredicateDto predicateDto = PredicateUtils.convertToDto(predicate,
                                                                    JournalServiceImpl.PredicateDto.class);

        RecordsQuery alfQuery = new RecordsQuery();
        alfQuery.setSourceId("alfresco/");
        alfQuery.setLanguage("fts-alfresco");
        alfQuery.setConsistency(QueryConsistency.EVENTUAL);
        String query = "TYPE:\"journal:journal\"";

        boolean hasLabel = predicateDto != null && StringUtils.isNotBlank(predicateDto.getLabel());
        boolean hasModuleId = predicateDto != null && StringUtils.isNotBlank(predicateDto.getModuleId());
        if (hasLabel || hasModuleId) {
            query += " AND (";
            if (hasModuleId) {
                query += "@journal:journalType:\"*" + predicateDto.getModuleId() + "*\"";
                if (hasLabel) {
                    query += " OR ";
                }
            }
            if (hasLabel) {
                query += "@cm:title:\"*" + predicateDto.getLabel() + "*\"";
            }
            query += ")";
        }
        alfQuery.setQuery(query);
        alfQuery.setMaxItems(recordsQuery.getMaxItems() - result.getRecords().size());
        alfQuery.setSkipCount((int) Math.max(0, recordsQuery.getSkipCount() - result.getTotalCount()));

        RecordsQueryResult<AlfJournalMeta> alfMeta = recordsService.queryRecords(alfQuery, AlfJournalMeta.class);

        result.addRecords(alfMeta.getRecords()
            .stream()
            .map(rec -> Json.getMapper().convert(rec, JournalWithMeta.class))
            .map(JournalRecordsDao.JournalRecord::new)
            .collect(Collectors.toList()));
        result.setTotalCount(alfMeta.getTotalCount() + result.getTotalCount());

        return result;
    }

    @Data
    public static class AlfJournalMeta {
        @AttName(".disp")
        private String label;
        @AttName("journal:journalType")
        private String id;
    }

    @Override
    public String getId() {
        return "journal_all";
    }
}
