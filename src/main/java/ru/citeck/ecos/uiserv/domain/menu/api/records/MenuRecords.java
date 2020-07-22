package ru.citeck.ecos.uiserv.domain.menu.api.records;

import ecos.com.fasterxml.jackson210.annotation.JsonProperty;
import ecos.com.fasterxml.jackson210.annotation.JsonValue;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDao;
import ru.citeck.ecos.records2.source.dao.local.MutableRecordsLocalDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDao;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDao;
import ru.citeck.ecos.uiserv.domain.menu.service.MenuService;
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDto;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MenuRecords extends LocalRecordsDao
    implements LocalRecordsMetaDao<MenuRecords.MenuRecord>,
               LocalRecordsQueryWithMetaDao<Object>,
               MutableRecordsLocalDao<MenuRecords.MenuRecord> {

    private static final String ID = "menu";
    private static final String AUTHORITIES_QUERY_LANG = "authorities";

    private final MenuService menuService;

    {
        setId(ID);
    }

    @NotNull
    @Override
    public List<MenuRecord> getValuesToMutate(@NotNull List<RecordRef> records) {
        return getLocalRecordsMeta(records, null);
    }

    @NotNull
    @Override
    public RecordsQueryResult<Object> queryLocalRecords(@NotNull RecordsQuery recordsQuery,
                                                        @NotNull MetaField metaField) {

        if (AUTHORITIES_QUERY_LANG.equals(recordsQuery.getLanguage())) {
            return new RecordsQueryResult<>(new ArrayList<>(menuService.getAllAuthoritiesWithMenu()));
        }

        if (!"predicate".equals(recordsQuery.getLanguage())
            && !"criteria".equals(recordsQuery.getLanguage())) {

            MenuQuery menuQuery = recordsQuery.getQuery(MenuQuery.class);
            if (menuQuery != null && StringUtils.isNotBlank(menuQuery.user)) {
                MenuDto menuDto = menuService.getMenuForUser(menuQuery.user);
                return RecordsQueryResult.of(new MenuRecord(menuDto));
            }
        }

        RecordsQueryResult<Object> result = new RecordsQueryResult<>();
        result.setRecords(menuService.getAllMenus()
            .stream()
            .map(MenuRecord::new)
            .collect(Collectors.toList()));

        return result;
    }

    @Override
    public RecordsMutResult save(List<MenuRecord> values) {

        RecordsMutResult result = new RecordsMutResult();
        values.forEach(dto -> {
            if (StringUtils.isBlank(dto.getId())) {
                throw new IllegalArgumentException("Parameter 'id' is mandatory for menu record");
            }

            if (dto.getId().equals("default-menu")) {
                dto.setId(UUID.randomUUID().toString());
            }

            MenuDto saved = menuService.save(dto);
            result.addRecord(new RecordMeta(saved.getId()));
        });

        return result;
    }

    @Override
    public RecordsDelResult delete(RecordsDeletion deletion) {
        RecordsDelResult result = new RecordsDelResult();
        deletion.getRecords().stream()
            .map(RecordRef::getId)
            .forEach(id -> {
                menuService.deleteByExtId(id);
                result.addRecord(new RecordMeta(id));
            });
        return result;
    }

    @Override
    public List<MenuRecord> getLocalRecordsMeta(List<RecordRef> records, MetaField metaField) {
        return records.stream()
            .map(RecordRef::getId)
            .map(id -> menuService.getMenu(id)
                .orElseGet(() -> new MenuDto(id)))
            .map(MenuRecord::new)
            .collect(Collectors.toList());
    }

    @Data
    public static class MenuQuery {
        private String user;
    }

    public static class MenuRecord extends MenuDto {

        public MenuRecord(MenuDto model) {
            super(model);
        }

        public MenuRecord() {
        }

        public String getModuleId() {
            return getId();
        }

        public void setModuleId(String value) {
            setId(value);
        }

        @MetaAtt(".disp")
        public String getDisplayName() {
            return getId();
        }

        @JsonProperty("_content")
        public void setContent(List<ObjectData> content) {

            String base64Content = content.get(0).get("url", "");
            base64Content = base64Content.replaceAll("^data:application/json;base64,", "");
            ObjectData data = Json.getMapper().read(Base64.getDecoder().decode(base64Content), ObjectData.class);

            Json.getMapper().applyData(this, data);
        }

        @JsonValue
        @com.fasterxml.jackson.annotation.JsonValue
        public MenuDto toJson() {
            return new MenuDto(this);
        }
    }
}
