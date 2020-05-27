package ru.citeck.ecos.uiserv.service.menu;

import ecos.com.fasterxml.jackson210.annotation.JsonProperty;
import ecos.com.fasterxml.jackson210.annotation.JsonValue;
import org.apache.commons.lang.StringUtils;
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
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.MutableRecordsLocalDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsQueryWithMetaDAO;
import ru.citeck.ecos.uiserv.service.menu.dto.MenuDto;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MenuRecords extends LocalRecordsDAO
    implements LocalRecordsMetaDAO<MenuRecords.MenuRecord>,
               LocalRecordsQueryWithMetaDAO<MenuRecords.MenuRecord>,
               MutableRecordsLocalDAO<MenuRecords.MenuRecord> {

    private static final String ID = "menu";
    private final MenuService menuService;

    public MenuRecords(MenuService menuService) {
        setId(ID);
        this.menuService = menuService;
    }

    @Override
    public List<MenuRecord> getValuesToMutate(List<RecordRef> records) {
        return getLocalRecordsMeta(records, null);
    }

    @Override
    public RecordsQueryResult<MenuRecord> queryLocalRecords(RecordsQuery recordsQuery, MetaField metaField) {

        if ("criteria".equals(recordsQuery.getLanguage())
            || "predicate".equals(recordsQuery.getLanguage())) {

            RecordsQueryResult<MenuRecord> result = new RecordsQueryResult<>();
            result.setRecords(menuService.getAllMenus()
                .stream()
                .map(MenuRecord::new)
                .collect(Collectors.toList()));

            return result;
        }

        return new RecordsQueryResult<>();
    }

    @Override
    public RecordsMutResult save(List<MenuRecord> values) {

        RecordsMutResult result = new RecordsMutResult();
        values.forEach(dto -> {
            if (StringUtils.isBlank(dto.getId())) {
                throw new IllegalArgumentException("Parameter 'id' is mandatory for menu record");
            }

            //todo: delete this when editor will be ready
            if (dto.getId().equals("default-menu")) {
                throw new IllegalArgumentException("Default menu can't be changed yet. Please, chose other ID");
            }
            dto.setAuthorities(Collections.emptyList());
            //===========================================

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
