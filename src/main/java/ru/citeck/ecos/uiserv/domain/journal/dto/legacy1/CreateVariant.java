package ru.citeck.ecos.uiserv.domain.journal.dto.legacy1;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class CreateVariant {

    private String title;
    private String destination;
    private String type;
    private String formId;
    private boolean isDefault;
    private boolean canCreate;
    private String createArguments;
    private String recordRef;
    private String formKey;
    private Map<String, String> attributes = new HashMap<>();
}
