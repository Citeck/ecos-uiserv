package ru.citeck.ecos.uiserv.domain.journal.dto.legacy1;

import lombok.Data;

import java.util.Map;

@Data
public class GroupAction {
    private String id;
    private String title;
    private Map<String, String> params;
    private String type;
    private String formKey;
}
