package ru.citeck.ecos.uiserv.journal.dto.legacy1;

import lombok.Data;

import java.util.Map;

@Data
class GroupAction {
    private String id;
    private String title;
    private Map<String, String> params;
    private String type;
    private String formKey;
}
