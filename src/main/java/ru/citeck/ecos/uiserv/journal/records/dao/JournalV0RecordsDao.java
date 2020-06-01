package ru.citeck.ecos.uiserv.journal.records.dao;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.request.query.QueryConsistency;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDAO;
import ru.citeck.ecos.uiserv.journal.dto.JournalDto;
import ru.citeck.ecos.uiserv.journal.dto.legacy0.JournalConfig;
import ru.citeck.ecos.uiserv.journal.dto.legacy0.JournalConfigResp;
import ru.citeck.ecos.uiserv.journal.dto.legacy0.JournalTypeDto;
import ru.citeck.ecos.uiserv.security.SecurityUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JournalV0RecordsDao extends LocalRecordsDAO
    implements LocalRecordsQueryWithMetaDAO<JournalConfig>,
    LocalRecordsMetaDAO<JournalConfig> {

    private static final String PROXY_URI = "http://alfresco/share/proxy/alfresco/";
    private static final String CONFIG_URI = PROXY_URI + "api/journals/journals-config?nodeRef=";
    private static final String TYPE_URI = PROXY_URI + "api/journals/types/";
    private static final String NODE_REF_PREFIX = "workspace://";

    private final JournalRecordsDao journalRecordsDao;
    private final JournalV0Format converter;
    private final RecordsService recordsService;

    private LoadingCache<ConfigKey, Optional<JournalConfigResp>> configByJournalType;
    private LoadingCache<ConfigKey, Optional<JournalConfigResp>> configByNodeRef;
    private LoadingCache<ConfigKey, Optional<JournalTypeDto>> journalTypeInfoById;

    @Qualifier("recordsRestTemplate")
    private final RestTemplate recordsRestTemplate;

    @PostConstruct
    public void init() {

        configByJournalType = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .maximumSize(100)
            .build(CacheLoader.from(this::getConfigByTypeImpl));

        configByNodeRef = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .maximumSize(100)
            .build(CacheLoader.from(this::getConfigByNodeRefImpl));

        journalTypeInfoById = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .maximumSize(100)
            .build(CacheLoader.from(this::getJournalTypeInfoImpl));
    }

    @Override
    public RecordsQueryResult<JournalConfig> queryLocalRecords(RecordsQuery recordsQuery, MetaField metaField) {
        RecordsQueryResult<JournalDto> journalsResult = journalRecordsDao.queryLocalRecords(recordsQuery, metaField);
        return new RecordsQueryResult<>(journalsResult, converter::convert);
    }

    @Override
    public List<JournalConfig> getLocalRecordsMeta(List<RecordRef> list, MetaField metaField) {

        String user = SecurityUtils.getCurrentUserLoginFromRequestContext();
        Locale locale = LocaleContextHolder.getLocale();
        ConfigKey configKey = new ConfigKey(user, locale, "");

        Map<RecordRef, JournalConfigResp> configByRefs = getConfigByRefs(configKey, list);
        List<RecordRef> refsForConfigQuery = mapRefsToTypeRefs(list, configByRefs);

        List<JournalConfig> result = new ArrayList<>();

        List<JournalDto> journals = journalRecordsDao.getLocalRecordsMeta(refsForConfigQuery, metaField);
        for (int i = 0; i < journals.size(); i++) {

            JournalDto dto = journals.get(i);
            RecordRef ref = list.get(i);

            if (StringUtils.isBlank(dto.getId())) {

                JournalConfig res = null;

                String journalTypeId = null;
                JournalConfigResp config = configByRefs.get(ref);
                if (ref.getId().startsWith(NODE_REF_PREFIX)) {
                    if (config != null) {
                        journalTypeId = config.getType();
                    }
                } else {
                    journalTypeId = ref.getId();
                }

                if (config == null && journalTypeId != null) {
                    config = configByJournalType.getUnchecked(configKey.withKey(journalTypeId)).orElse(null);
                }

                JournalTypeDto typeDto = null;
                if (StringUtils.isNotBlank(journalTypeId)) {
                    typeDto = journalTypeInfoById.getUnchecked(configKey.withKey(journalTypeId)).orElse(null);
                }

                if (typeDto != null || config != null) {
                    res = new JournalConfig(config, typeDto);
                }

                result.add(res != null ? res : converter.convert(dto));

            } else {

                result.add(converter.convert(dto));
            }
        }

        return result;
    }

    private List<RecordRef> mapRefsToTypeRefs(List<RecordRef> refs, Map<RecordRef, JournalConfigResp> configs) {

        return refs.stream().map(ref -> {
            JournalConfigResp config = configs.get(ref);
            if (config != null && StringUtils.isNotBlank(config.getType())) {
                return RecordRef.create(ref.getAppName(), ref.getSourceId(), config.getType());
            }
            return ref;
        }).collect(Collectors.toList());
    }


    private Map<RecordRef, JournalConfigResp> getConfigByRefs(ConfigKey configKey, List<RecordRef> refs) {

        Map<RecordRef, JournalConfigResp> result = new HashMap<>();
        refs.forEach(ref ->
            configByNodeRef.getUnchecked(configKey.withKey(ref.getId())).ifPresent(config -> result.put(ref, config))
        );
        return result;
    }

    private Optional<JournalTypeDto> getJournalTypeInfoImpl(ConfigKey key) {

        String journalType = key.getKey();

        if (StringUtils.isBlank(journalType)) {
            return Optional.empty();
        }
        try {
            JournalTypeDto typeDto = recordsRestTemplate.getForObject(TYPE_URI + journalType, JournalTypeDto.class);
            if (typeDto != null && StringUtils.isNotBlank(typeDto.getId())) {
                return Optional.of(typeDto);
            }
        } catch (Exception e) {
            log.error("Journal type info can't be received. Type: '" + journalType + "'", e);
        }
        return Optional.empty();
    }

    private Optional<JournalConfigResp> getConfigByTypeImpl(ConfigKey key) {

        String journalType = key.getKey();

        RecordsQuery query = new RecordsQuery();
        query.setSourceId("alfresco/");
        query.setLanguage("fts-alfresco");
        query.setConsistency(QueryConsistency.EVENTUAL);
        query.setMaxItems(1);
        query.setQuery("TYPE:\"journal:journal\" AND =journal:journalType:\"" + journalType + "\"");
        try {
            RecordRef journalRef = recordsService.queryRecord(query).orElse(null);
            if (RecordRef.isEmpty(journalRef)) {
                return Optional.empty();
            } else {
                return configByNodeRef.getUnchecked(key.withKey(journalRef.getId()));
            }
        } catch (Exception e) {
            log.error("Journal config can't be received. Type: " + journalType, e);
        }
        return Optional.empty();
    }

    private Optional<JournalConfigResp> getConfigByNodeRefImpl(ConfigKey key) {

        String nodeRef = key.getKey();

        if (StringUtils.isBlank(nodeRef) || !nodeRef.startsWith(NODE_REF_PREFIX)) {
            return Optional.empty();
        }
        String uri = CONFIG_URI + nodeRef;
        try {
            JournalConfigResp config = recordsRestTemplate.getForObject(uri, JournalConfigResp.class);
            if (config != null && StringUtils.isNotBlank(config.getNodeRef())) {
                return Optional.of(config);
            }
        } catch (Exception e) {
            log.error("Journal config can't be received. Ref: '" + nodeRef + "'", e);
        }
        return Optional.empty();
    }

    @Override
    public String getId() {
        return "journal_v0";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigKey {

        private String user;
        private Locale locale;
        private String key;

        public ConfigKey withKey(String key) {
            return new ConfigKey(user, locale, key);
        }
    }
}
