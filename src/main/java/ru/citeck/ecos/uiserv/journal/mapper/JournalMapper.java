package ru.citeck.ecos.uiserv.journal.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.journal.domain.JournalEntity;
import ru.citeck.ecos.uiserv.journal.dto.JournalColumnDto;
import ru.citeck.ecos.uiserv.journal.dto.JournalDto;
import ru.citeck.ecos.uiserv.journal.dto.JournalSortBy;
import ru.citeck.ecos.uiserv.journal.repository.JournalRepository;

import java.util.ArrayList;

@Component
@RequiredArgsConstructor
@Slf4j
public class JournalMapper {

    private final JournalRepository repository;

    public JournalDto entityToDto(JournalEntity entity) {

        JournalDto dto = new JournalDto();
        dto.setId(entity.getExtId());
        dto.setEditable(entity.getEditable());
        dto.setColumns(Json.getMapper().read(entity.getColumns(), ColumnsList.class));
        dto.setLabel(Json.getMapper().read(entity.getLabel(), MLText.class));
        dto.setTypeRef(RecordRef.valueOf(entity.getTypeRef()));
        dto.setPredicate(Json.getMapper().read(entity.getPredicate(), ObjectData.class));
        dto.setActions(Json.getMapper().read(entity.getActions(), RecordRefsList.class));
        dto.setAttributes(Json.getMapper().read(entity.getAttributes(), ObjectData.class));
        dto.setMetaRecord(RecordRef.valueOf(entity.getMetaRecord()));
        dto.setSourceId(entity.getSourceId());
        dto.setGroupBy(Json.getMapper().read(entity.getGroupBy(), StrList.class));
        dto.setSortBy(Json.getMapper().read(entity.getSortBy(), SortByList.class));

        return dto;
    }

    public JournalEntity dtoToEntity(JournalDto dto) {

        JournalEntity entity = repository.findByExtId(dto.getId()).orElse(null);
        if (entity == null) {
            entity = new JournalEntity();
            entity.setExtId(dto.getId());
        }

        entity.setEditable(dto.getEditable());
        entity.setColumns(Json.getMapper().toString(dto.getColumns()));
        entity.setLabel(Json.getMapper().toString(dto.getLabel()));
        entity.setTypeRef(RecordRef.toString(dto.getTypeRef()));
        entity.setPredicate(Json.getMapper().toString(dto.getPredicate()));
        entity.setActions(Json.getMapper().toString(dto.getActions()));
        entity.setAttributes(Json.getMapper().toString(dto.getAttributes()));
        entity.setMetaRecord(RecordRef.toString(dto.getMetaRecord()));
        entity.setSourceId(dto.getSourceId());
        entity.setGroupBy(Json.getMapper().toString(dto.getGroupBy()));
        entity.setSortBy(Json.getMapper().toString(dto.getSortBy()));

        return entity;
    }

    public static class ColumnsList extends ArrayList<JournalColumnDto> {}
    public static class RecordRefsList extends ArrayList<RecordRef> {}
    public static class StrList extends ArrayList<String> {}
    public static class SortByList extends ArrayList<JournalSortBy> {}
}
