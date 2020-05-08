package ru.citeck.ecos.uiserv.journal.dto.legacy0;

import lombok.Data;

import java.util.List;

@Data
public class JournalConfigResp {

    private String type;
    private List<CreateVariant> createVariants;
    private List<Criteria> criteria;
    private String nodeRef;
    private String title;
}
