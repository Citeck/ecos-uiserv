package ru.citeck.ecos.uiserv.domain.journal.service.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.domain.journal.dto.*;
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalEntity;
import ru.citeck.ecos.uiserv.domain.journal.dto.legacy1.GroupAction;
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalRepository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JournalMapper {

    private final JournalRepository repository;

    public JournalWithMeta entityToDto(JournalEntity entity) {

        JournalWithMeta dto = new JournalWithMeta();
        dto.setId(entity.getExtId());
        dto.setEditable(entity.getEditable());

        ColumnsList columns = Json.getMapper().read(entity.getColumns(), ColumnsList.class);
        dto.setColumns(columns != null ? columns : Collections.emptyList());
        dto.setLabel(Json.getMapper().read(entity.getLabel(), MLText.class));
        dto.setTypeRef(RecordRef.valueOf(entity.getTypeRef()));
        dto.setPredicate(Json.getMapper().read(entity.getPredicate(), ObjectData.class));
        dto.setQueryData(Json.getMapper().read(entity.getQueryData(), ObjectData.class));
        dto.setActions(Json.getMapper().read(entity.getActions(), RecordRefsList.class));
        dto.setAttributes(Json.getMapper().read(entity.getAttributes(), ObjectData.class));
        dto.setMetaRecord(RecordRef.valueOf(entity.getMetaRecord()));
        dto.setSourceId(entity.getSourceId());
        dto.setGroupBy(Json.getMapper().read(entity.getGroupBy(), StrList.class));
        dto.setSortBy(Json.getMapper().read(entity.getSortBy(), SortByList.class));
        dto.setGroupActions(Json.getMapper().read(entity.getGroupActions(), GroupActionsList.class));
        dto.setCreateVariants(Json.getMapper().read(entity.getCreateVariants(), CreateVariantsList.class));
        dto.setComputed(Json.getMapper().read(entity.getComputed(), ComputedParamsList.class));

        dto.setModified(entity.getLastModifiedDate());
        dto.setModifier(entity.getLastModifiedBy());
        dto.setCreated(entity.getCreatedDate());
        dto.setCreator(entity.getCreatedBy());

        if (dto.getAttributes() == null) {
            dto.setAttributes(ObjectData.create());
        }
        if (dto.getEditable() == null) {
            dto.setEditable(true);
        }
        if (dto.getPredicate() == null) {
            dto.setPredicate(ObjectData.create());
        }

        dto.getColumns().forEach(c -> {
            if (c.getVisible() == null) {
                c.setVisible(true);
            }
            if (c.getEditable() == null) {
                c.setEditable(true);
            }
            if (c.getGroupable() == null) {
                c.setGroupable(true);
            }
            if (c.getSearchable() == null) {
                c.setSearchable(true);
            }
            if (c.getSortable() == null) {
                c.setSortable(true);
            }
            if (c.getAttributes() == null) {
                c.setAttributes(ObjectData.create());
            }
        });

        return dto;
    }

    public JournalEntity dtoToEntity(JournalDto dto) {

        JournalEntity entity = repository.findByExtId(dto.getId()).orElse(null);
        if (entity == null) {
            entity = new JournalEntity();
            entity.setExtId(dto.getId());
        }

        entity.setEditable(dto.getEditable());
        entity.setColumns(Json.getMapper().toString(getNotBlank(dto.getColumns(), JournalColumnDto::getName)));
        entity.setLabel(Json.getMapper().toString(dto.getLabel()));
        entity.setTypeRef(RecordRef.toString(dto.getTypeRef()));
        entity.setPredicate(Json.getMapper().toString(dto.getPredicate()));
        entity.setQueryData(Json.getMapper().toString(dto.getQueryData()));
        entity.setActions(Json.getMapper().toString(dto.getActions()));
        entity.setAttributes(Json.getMapper().toString(dto.getAttributes()));
        entity.setMetaRecord(RecordRef.toString(dto.getMetaRecord()));
        entity.setSourceId(dto.getSourceId());
        entity.setGroupBy(Json.getMapper().toString(getNotBlank(dto.getGroupBy(), v -> v)));
        entity.setGroupActions(Json.getMapper().toString(getNotBlank(dto.getGroupActions(), GroupAction::getId)));
        entity.setSortBy(Json.getMapper().toString(getNotBlank(dto.getSortBy(), JournalSortBy::getAttribute)));
        entity.setCreateVariants(Json.getMapper().toString(getNotBlank(dto.getCreateVariants(),
            v -> RecordRef.valueOf(v.getRecordRef()).toString())));
        entity.setComputed(Json.getMapper().toString(getNotBlank(dto.getComputed(),
            ComputedParamDto::getName)));

        return entity;
    }

    private <T> List<T> getNotBlank(List<T> list, Function<T, String> getValueToCheck) {
        if (list == null) {
            return null;
        }
        return list.stream()
            .filter(element -> StringUtils.isNotBlank(getValueToCheck.apply(element)))
            .collect(Collectors.toList());
    }

    public static class ColumnsList extends ArrayList<JournalColumnDto> {}
    public static class RecordRefsList extends ArrayList<RecordRef> {}
    public static class StrList extends ArrayList<String> {}
    public static class SortByList extends ArrayList<JournalSortBy> {}
    public static class GroupActionsList extends ArrayList<GroupAction> {}
    public static class CreateVariantsList extends ArrayList<CreateVariantDto> {}
    public static class ComputedParamsList extends ArrayList<ComputedParamDto> {}
}
