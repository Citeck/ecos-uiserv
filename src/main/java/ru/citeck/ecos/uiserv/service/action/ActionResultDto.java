package ru.citeck.ecos.uiserv.service.action;

import lombok.Data;
import ru.citeck.ecos.commons.data.MLText;

@Data
public class ActionResultDto {
    private MLText title;
    private MLText message;
}
