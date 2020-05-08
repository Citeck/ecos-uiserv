package ru.citeck.ecos.uiserv.journal.dto.legacy0;

import lombok.Data;

@Data
public class CreateVariant {

    private Boolean canCreate;
    private String createArguments;
    private String destination;
    private String formId;
    private String formKey;
    private Boolean isDefault;
    private String recordRef;
    private String title;
    private String type;
}
