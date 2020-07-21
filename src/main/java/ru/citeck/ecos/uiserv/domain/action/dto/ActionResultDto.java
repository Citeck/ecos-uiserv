package ru.citeck.ecos.uiserv.domain.action.dto;

import lombok.Data;
import ru.citeck.ecos.commons.data.MLText;

@Data
public class ActionResultDto {
    private MLText title;
    private MLText message;
}
