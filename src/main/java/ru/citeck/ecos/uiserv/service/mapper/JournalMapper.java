package ru.citeck.ecos.uiserv.service.mapper;

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
import ru.citeck.ecos.uiserv.domain.journal.JournalColumnEntity;
import ru.citeck.ecos.uiserv.domain.journal.JournalEntity;
import ru.citeck.ecos.uiserv.dto.journal.JournalColumnDto;
import ru.citeck.ecos.uiserv.dto.journal.JournalDto;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JournalMapper {

    private final static String APP_NAME = "uiserv";
    private final static String ACTION_SOURCE_ID = "action";

    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;
    private final JournalColumnMapper columnMapper;

    public JournalDto entityToDto(JournalEntity entity) {

        JournalDto dto = modelMapper.map(entity, JournalDto.class);

        if (StringUtils.isNotEmpty(entity.getName())) {
            dto.setName(Json.getMapper().read(entity.getName(), MLText.class));
        }

        if (StringUtils.isNotEmpty(entity.getTypeRef())) {
            dto.setTypeRef(RecordRef.valueOf(entity.getTypeRef()));
        }

        if (entity.getPredicate() != null) {
            try {
                dto.setPredicate(objectMapper.readTree(entity.getPredicate()));
            } catch (IOException e) {
                log.error("Cannot parse predicate to JsonNode for journal with EXT_ID: " + entity.getExtId(), e);
            }
        }

        if (CollectionUtils.isNotEmpty(entity.getActions())) {
            Set<RecordRef> actionsRefs = entity.getActions().stream()
                .map(a -> RecordRef.create(APP_NAME, ACTION_SOURCE_ID, a.getExtId()))
                .collect(Collectors.toSet());
            dto.setActions(actionsRefs);
        }

        if (StringUtils.isNotEmpty(entity.getAttributes())) {
            ObjectData attributes = new ObjectData(entity.getAttributes());
            dto.setAttributes(attributes);
        }

        Set<JournalColumnEntity> columnEntities = entity.getColumns();
        if (CollectionUtils.isNotEmpty(columnEntities)) {
            Set<JournalColumnDto> columnDtos = columnEntities.stream()
                .map(columnMapper::entityToDto)
                .collect(Collectors.toSet());
            dto.setColumns(columnDtos);
        }

        return dto;
    }

    public JournalEntity dtoToEntity(JournalDto dto) {
        return modelMapper.map(dto, JournalEntity.class);
    }
}
