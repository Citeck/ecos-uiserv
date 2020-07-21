package ru.citeck.ecos.uiserv.domain.journal.dto.legacy0;

import lombok.Data;

@Data
public class Criteria {
    private String field;
    private String persistedValue;
    private String predicate;
}
