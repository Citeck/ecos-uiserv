package ru.citeck.ecos.uiserv.journal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JournalSortBy {
    private String attribute;
    private boolean ascending;
}
