package ru.citeck.ecos.uiserv.journal.eapps.mapper;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.uiserv.journal.dto.JournalColumnDto;
import ru.citeck.ecos.uiserv.journal.dto.JournalDto;
import ru.citeck.ecos.uiserv.journal.eapps.module.OldJournalModule;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OldJournalMapper {

    private final ModelMapper modelMapper;

    public JournalDto moduleToDto(OldJournalModule module) {
        JournalDto dto = modelMapper.map(module, JournalDto.class);

        String name = module.getName();
        if (StringUtils.isNotEmpty(name)) {
            dto.setName(new MLText(name));
        }

        String columnsJSON = module.getColumnsJSONStr();
        if (StringUtils.isNotEmpty(columnsJSON)) {
            List<JournalColumnDto> columns = Json.getMapper().read(columnsJSON, JournalColumnDtoList.class);
            dto.setColumns(columns);
        }

        return dto;
    }

    private static class JournalColumnDtoList extends ArrayList<JournalColumnDto> {
    }
}
