package ru.citeck.ecos.uiserv.domain.form.service;

import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormModel;

public interface MutableFormProvider {
    void save(EcosFormModel model);

    void create(EcosFormModel model);

    void delete(String formId);
}
