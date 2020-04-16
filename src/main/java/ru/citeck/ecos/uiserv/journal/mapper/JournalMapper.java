package ru.citeck.ecos.uiserv.journal.mapper;

import ecos.com.fasterxml.jackson210.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.journal.domain.JournalEntity;
import ru.citeck.ecos.uiserv.journal.dto.JournalDto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JournalMapper {

    private final ModelMapper modelMapper;

    public JournalDto entityToDto(JournalEntity entity) {

        JournalDto dto = modelMapper.map(entity, JournalDto.class);

        if (StringUtils.isNotEmpty(entity.getName())) {
            dto.setName(Json.getMapper().read(entity.getName(), MLText.class));
        }

        if (StringUtils.isNotEmpty(entity.getTypeRef())) {
            dto.setTypeRef(RecordRef.valueOf(entity.getTypeRef()));
        }

        if (entity.getPredicate() != null) {
            ObjectData objectData = Json.getMapper().read(entity.getPredicate(), ObjectData.class);
            dto.setPredicate(objectData);
        }

        if (StringUtils.isNotEmpty(entity.getActions())) {
            List<RecordRef> actionsRefs = Json.getMapper().read(entity.getActions(), RecordRefsList.class);
            dto.setActions(actionsRefs);
        }

        if (StringUtils.isNotEmpty(entity.getAttributes())) {
            ObjectData attributes = new ObjectData(entity.getAttributes());
            dto.setAttributes(attributes);
        }

        if (entity.getMetaRecord() != null) {
            dto.setMetaRecord(RecordRef.valueOf(entity.getMetaRecord()));
        }

        return dto;
    }

    public JournalEntity dtoToEntity(JournalDto dto) {
        return modelMapper.map(dto, JournalEntity.class);
    }

    public static class RecordRefsList extends ArrayList<RecordRef> {}
}
