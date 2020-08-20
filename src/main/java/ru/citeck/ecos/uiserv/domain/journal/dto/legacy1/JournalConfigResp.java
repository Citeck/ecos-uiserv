package ru.citeck.ecos.uiserv.domain.journal.dto.legacy1;

import lombok.Data;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.uiserv.domain.journal.dto.ComputedParamDto;

import java.util.List;
import java.util.Map;

@Data
public class JournalConfigResp {

    private String id;
    private String sourceId;
    private JournalMeta meta;
    private List<Column> columns;
    private Map<String, String> params;
    private List<ComputedParamDto> computed;

    @MetaAtt(".disp")
    public String getDisplayName() {
        if (meta == null) {
            return "Journal";
        }
        return meta.getTitle();
    }
}
