package ru.citeck.ecos.uiserv.domain.form.service;

import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef;

public interface MutableFormProvider {
    void save(EcosFormDef model);

    void create(EcosFormDef model);

    void delete(String formId);
}
