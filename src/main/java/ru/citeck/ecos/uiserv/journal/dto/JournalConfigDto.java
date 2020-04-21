package ru.citeck.ecos.uiserv.journal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.citeck.ecos.commons.data.ObjectData;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JournalConfigDto {

    private String type;

    private ObjectData config;
}
