package ru.citeck.ecos.uiserv.service.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.domain.journal.JournalColumnEntity;
import ru.citeck.ecos.uiserv.domain.journal.JournalConfigEntity;
import ru.citeck.ecos.uiserv.domain.journal.JournalEntity;
import ru.citeck.ecos.uiserv.dto.journal.JournalColumnDto;
import ru.citeck.ecos.uiserv.dto.journal.JournalConfigDto;
import ru.citeck.ecos.uiserv.dto.journal.JournalDto;

@Component
@RequiredArgsConstructor
@Slf4j
public class JournalColumnMapper {

    private final ModelMapper modelMapper;

    public JournalColumnDto entityToDto(JournalColumnEntity entity) {

        JournalColumnDto dto = modelMapper.map(entity, JournalColumnDto.class);

        if (StringUtils.isNotEmpty(entity.getName())) {
            dto.setName(Json.getMapper().read(entity.getName(), MLText.class));
        }

        if (StringUtils.isNotEmpty(entity.getType())) {
            dto.setType(entity.getType());
        }

        if (StringUtils.isNotEmpty(entity.getEditorRef())) {
            dto.setEditorRef(RecordRef.valueOf(entity.getEditorRef()));
        }

        if (StringUtils.isNotEmpty(entity.getAttributes())) {
            ObjectData attributes = new ObjectData(entity.getAttributes());
            dto.setAttributes(attributes);
        }

        JournalConfigEntity formatter = entity.getFormatter();
        if (formatter != null) {
            JournalConfigDto formatterDto = new JournalConfigDto();

            formatterDto.setType(formatter.getType());
            formatterDto.setConfig(new ObjectData(formatter.getConfig()));
        }

        JournalConfigEntity filter = entity.getFilter();
        if (filter != null) {
            JournalConfigDto filterDto = new JournalConfigDto();

            filterDto.setType(filter.getType());
            filterDto.setConfig(new ObjectData(filter.getConfig()));
        }

        JournalConfigEntity options = entity.getOptions();
        if (options != null) {
            JournalConfigDto optionsDto = new JournalConfigDto();

            optionsDto.setType(options.getType());
            optionsDto.setConfig(new ObjectData(options.getConfig()));
        }

        return dto;
    }

    public JournalEntity dtoToEntity(JournalDto dto) {
        return modelMapper.map(dto, JournalEntity.class);
    }
}
