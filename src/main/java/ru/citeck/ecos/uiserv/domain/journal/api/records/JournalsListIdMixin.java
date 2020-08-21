package ru.citeck.ecos.uiserv.domain.journal.api.records;

import kotlin.Unit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.source.common.AttributesMixin;
import ru.citeck.ecos.uiserv.domain.journal.service.JournalService;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JournalsListIdMixin implements AttributesMixin<Unit, RecordRef> {

    private static final String ATT = "journalsListId";

    private final JournalService journalService;
    private final AllJournalRecordsDao allJournalRecordsDao;

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
        return journalService.getJournalsListIdByJournalId(meta.getId());
    }

    @Override
    public Unit getMetaToRequest() {
        return Unit.INSTANCE;
    }
}
