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
import ru.citeck.ecos.uiserv.journal.dto.legacy0.JournalConfig;
import ru.citeck.ecos.uiserv.journal.dto.legacy0.JournalConfigResp;
import ru.citeck.ecos.uiserv.journal.dto.legacy0.JournalTypeDto;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JournalV0RecordsDao extends LocalRecordsDAO
    implements LocalRecordsQueryWithMetaDAO<JournalConfig>,
    LocalRecordsMetaDAO<JournalConfig> {

    private final JournalRecordsDAO journalRecordsDao;
    private final JournalV0Format converter;

    @Qualifier("recordsRestTemplate")
    private final RestTemplate recordsRestTemplate;

    @Override
    public RecordsQueryResult<JournalConfig> queryLocalRecords(RecordsQuery recordsQuery, MetaField metaField) {
        RecordsQueryResult<JournalDto> journalsResult = journalRecordsDao.queryLocalRecords(recordsQuery, metaField);
        return new RecordsQueryResult<>(journalsResult, converter::convert);
    }

    @Override
    public List<JournalConfig> getLocalRecordsMeta(List<RecordRef> list, MetaField metaField) {

        List<JournalConfig> result = new ArrayList<>();

        List<JournalDto> journals = journalRecordsDao.getLocalRecordsMeta(list, metaField);
        for (int i = 0; i < journals.size(); i++) {

            JournalDto dto = journals.get(i);
            RecordRef ref = list.get(i);

            if (StringUtils.isBlank(dto.getId())) {

                JournalConfig res = null;

                try {
                    String proxyUrl = "http://alfresco/share/proxy/alfresco/";

                    JournalConfigResp config = recordsRestTemplate.getForObject(
                        proxyUrl + "api/journals/journals-config?nodeRef=" + ref.getId(),
                        JournalConfigResp.class);

                    if (config != null) {
                        JournalTypeDto typeDto = recordsRestTemplate.getForObject(
                            proxyUrl + "api/journals/types/" + config.getType(),
                            JournalTypeDto.class
                        );
                        if (typeDto != null) {
                            res = new JournalConfig(config, typeDto);
                        }
                    }
                } catch (Exception e) {
                    log.error("JournalConfig query failed. JournalId: '" + ref.getId() + "'", e);
                }
                result.add(res != null ? res : converter.convert(dto));
            } else {
                result.add(converter.convert(dto));
            }
        }

        return result;
    }

    @Override
    public String getId() {
        return "journal_v0";
    }
}
