package ru.citeck.ecos.uiserv.journal.dto.legacy1;

import lombok.Data;

@Data
class Criterion {
    private String field;
    private String predicate;
    private String value;
}
