package ru.citeck.ecos.uiserv.journal.records.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDAO;
import ru.citeck.ecos.uiserv.journal.dto.JournalDto;
import ru.citeck.ecos.uiserv.journal.dto.legacy1.JournalConfigResp;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JournalV1RecordsDao  extends LocalRecordsDAO
    implements LocalRecordsQueryWithMetaDAO<JournalConfigResp>,
    LocalRecordsMetaDAO<JournalConfigResp> {

    private final JournalRecordsDAO journalRecordsDao;
    private final JournalV1Format converter;

    @Qualifier("recordsRestTemplate")
    private final RestTemplate recordsRestTemplate;

    @Override
    public RecordsQueryResult<JournalConfigResp> queryLocalRecords(RecordsQuery recordsQuery, MetaField metaField) {
        RecordsQueryResult<JournalDto> journalsResult = journalRecordsDao.queryLocalRecords(recordsQuery, metaField);
        return new RecordsQueryResult<>(journalsResult, converter::convert);
    }

    @Override
    public List<JournalConfigResp> getLocalRecordsMeta(List<RecordRef> list, MetaField metaField) {

        List<JournalConfigResp> result = new ArrayList<>();

        List<JournalDto> journals = journalRecordsDao.getLocalRecordsMeta(list, metaField);
        for (int i = 0; i < journals.size(); i++) {

            JournalDto dto = journals.get(i);
            RecordRef ref = list.get(i);

            if (StringUtils.isBlank(dto.getId())) {
                try {
                    result.add(recordsRestTemplate.getForObject(
                        "http://alfresco/share/proxy/alfresco/api/journals/config?journalId=" + ref.getId(),
                        JournalConfigResp.class
                    ));
                } catch (Exception e) {
                    log.error("JournalConfig query failed. JournalId: '" + ref.getId() + "'", e);
                    result.add(converter.convert(dto));
                }
            } else {
                result.add(converter.convert(dto));
            }
        }

        return result;
    }

    @Override
    public String getId() {
        return "journal_v1";
    }
}
