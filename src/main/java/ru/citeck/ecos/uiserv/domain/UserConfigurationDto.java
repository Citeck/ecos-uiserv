package ru.citeck.ecos.uiserv.domain;

import lombok.Data;
import ru.citeck.ecos.commons.data.DataValue;

import java.io.Serializable;
import java.time.Instant;

@Data
public class UserConfigurationDto implements Serializable {
    private String id;
    private String userName;
    private Instant creationTime;
    private DataValue data;
}
