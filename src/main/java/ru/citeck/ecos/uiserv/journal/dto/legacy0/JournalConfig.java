package ru.citeck.ecos.uiserv.journal.dto.legacy0;

import ecos.com.fasterxml.jackson210.annotation.JsonIgnore;
import ecos.com.fasterxml.jackson210.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JournalConfig {

    private JournalTypeDto type;
    private List<CreateVariant> createVariants;
    private List<Criteria> criteria;
    private String nodeRef;
    private String title;

    public JournalConfig(JournalConfigResp conf, JournalTypeDto type) {
        this.type = type;
        this.createVariants = conf.getCreateVariants();
        this.criteria = conf.getCriteria();
        this.nodeRef = conf.getNodeRef();
        this.title = conf.getTitle();
    }

    public JournalConfig() {
    }

    @JsonIgnore
    public String getId() {
        return nodeRef;
    }
}
