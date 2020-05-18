package ru.citeck.ecos.uiserv.service.menu;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.MutableRecordsLocalDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDAO;
import ru.citeck.ecos.uiserv.service.menu.dto.MenuDto;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MenuRecords extends LocalRecordsDAO
    implements LocalRecordsMetaDAO<MenuDto>,
    MutableRecordsLocalDAO<MenuDto> {

    private static final String ID = "menu";
    private final MenuService menuService;

    public MenuRecords(MenuService menuService) {
        setId(ID);
        this.menuService = menuService;
    }

    @Override
    public List<MenuDto> getValuesToMutate(List<RecordRef> records) {
        return getLocalRecordsMeta(records, null);
    }

    @Override
    public RecordsMutResult save(List<MenuDto> values) {
        RecordsMutResult result = new RecordsMutResult();
        values.forEach(dto -> {
            if (StringUtils.isBlank(dto.getId())) {
                throw new IllegalArgumentException("Parameter 'id' is mandatory for menu record");
            }

        });
        for (MenuDto value : values) {
            MenuDto saved = menuService.save(value);
            result.addRecord(new RecordMeta(saved.getId()));
        }
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
    public List<MenuDto> getLocalRecordsMeta(List<RecordRef> records, MetaField metaField) {
        return records.stream()
            .map(RecordRef::getId)
            .map(id -> menuService.getMenu(id)
                .orElseGet(() -> new MenuDto(id)))
            .collect(Collectors.toList());
    }
}
