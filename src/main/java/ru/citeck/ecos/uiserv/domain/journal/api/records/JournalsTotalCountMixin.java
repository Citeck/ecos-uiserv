package ru.citeck.ecos.uiserv.domain.journal.api.records;

import kotlin.Unit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.predicate.PredicateService;
import ru.citeck.ecos.records2.request.query.QueryConsistency;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.source.common.AttributesMixin;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalWithMeta;
import ru.citeck.ecos.uiserv.domain.journal.service.JournalService;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JournalsTotalCountMixin implements AttributesMixin<Unit, RecordRef> {

    private static final String ATT = "totalCount";

    private final JournalService journalService;
    private final AllJournalRecordsDao allJournalRecordsDao;
    private final RecordsService recordsService;

    @PostConstruct
    public void init() {
        allJournalRecordsDao.addAttributesMixin(this);
    }

    @Override
    public List<String> getAttributesList() {
        return Collections.singletonList(ATT);
    }

    @Override
    public Object getAttribute(String attribute, RecordRef meta, MetaField field) throws Exception {
        JournalWithMeta journal = journalService.getJournalById(meta.getId());
        StringBuilder sourceIdBuilder = new StringBuilder(journal.getSourceId());
        ObjectData journalQueryData = journal.getQueryData();

        RecordsQuery query = new RecordsQuery();

        if (sourceIdBuilder.length() == 0) {
            sourceIdBuilder.append("alfresco/");
        } else if (sourceIdBuilder.indexOf("/") == -1) {
            sourceIdBuilder.insert(0, "alfresco/");
        }

        if (journalQueryData == null || journalQueryData.getData().size() == 0) {
            query.setQuery(journal.getPredicate().getData());
            query.setLanguage(PredicateService.LANGUAGE_PREDICATE);
        } else {
            Map<String, DataValue> dataMap = new HashMap<>();
            dataMap.put("predicate", journal.getPredicate().getData());
            dataMap.put("data", journalQueryData.getData());
            query.setQuery(DataValue.create(dataMap));
            query.setLanguage("predicate-with-data");
        }

        query.setSourceId(sourceIdBuilder.toString());
        query.setConsistency(QueryConsistency.EVENTUAL);
        query.setMaxItems(1);
        query.setSkipCount(0);

        return recordsService.queryRecords(query).getTotalCount();
    }

    @Override
    public Unit getMetaToRequest() {
        return Unit.INSTANCE;
    }
}
