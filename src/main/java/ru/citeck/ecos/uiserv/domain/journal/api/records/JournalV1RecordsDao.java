package ru.citeck.ecos.uiserv.domain.journal.api.records;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDao;
import ru.citeck.ecos.uiserv.domain.journal.dto.ResolvedJournalDto;
import ru.citeck.ecos.uiserv.domain.journal.dto.legacy1.Column;
import ru.citeck.ecos.uiserv.domain.journal.service.format.JournalV1Format;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalWithMeta;
import ru.citeck.ecos.uiserv.domain.journal.dto.legacy1.JournalConfigResp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class JournalV1RecordsDao  extends LocalRecordsDao
    implements LocalRecordsQueryWithMetaDao<JournalConfigResp>,
    LocalRecordsMetaDao<JournalConfigResp> {

    private static final String CONFIG_URL = "http://alfresco/share/proxy/alfresco/api/journals/config?journalId=%s";

    private final ResolvedJournalRecordsDao journalRecordsDao;
    private final JournalV1Format converter;

    @Qualifier("recordsRestTemplate")
    private final RestTemplate recordsRestTemplate;

    @Override
    public RecordsQueryResult<JournalConfigResp> queryLocalRecords(@NotNull RecordsQuery recordsQuery,
                                                                   @NotNull MetaField metaField) {
        RecordsQueryResult<ResolvedJournalDto> journalsResult =
            journalRecordsDao.queryLocalRecords(recordsQuery, metaField);
        return new RecordsQueryResult<>(journalsResult, converter::convert);
    }

    @Override
    public List<JournalConfigResp> getLocalRecordsMeta(@NotNull List<RecordRef> list,
                                                       @NotNull MetaField metaField) {

        List<JournalConfigResp> result = new ArrayList<>();

        List<ResolvedJournalDto> journals = journalRecordsDao.getLocalRecordsMeta(list, metaField);
        for (int i = 0; i < journals.size(); i++) {

            JournalWithMeta dto = journals.get(i);
            RecordRef ref = list.get(i);

            if (StringUtils.isBlank(dto.getId())) {
                try {
                    JournalConfigResp configResp = recordsRestTemplate.getForObject(
                        String.format(CONFIG_URL, ref.getId()),
                        JournalConfigResp.class
                    );
                    if (configResp == null) {
                        result.add(converter.convert(dto));
                        continue;
                    }
                    boolean wasResolved = false;
                    if (!Objects.equals(configResp.getId(), ref.getId())) {
                        List<RecordRef> newRef = Collections.singletonList(RecordRef.valueOf(configResp.getId()));
                        List<ResolvedJournalDto> meta = journalRecordsDao.getLocalRecordsMeta(newRef, metaField);
                        if (meta.size() == 1 && StringUtils.isNotBlank(meta.get(0).getId())) {
                            result.add(converter.convert(meta.get(0)));
                            wasResolved = true;
                        }
                    }
                    if (!wasResolved) {
                        List<Column> columns = configResp.getColumns();
                        if (columns != null) {
                            columns.forEach(c -> {
                                if (!c.isVisible()) {
                                    c.setHidden(true);
                                }
                            });
                        }
                        result.add(configResp);
                    }
                } catch (Exception e) {

                    log.error("JournalConfig query failed. JournalId: '"
                        + ref.getId() + ". Msg: '" + e.getMessage() + "'");

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
