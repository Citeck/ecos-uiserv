package ru.citeck.ecos.uiserv.service.form;

import ru.citeck.ecos.uiserv.domain.EcosFormModel;

public interface MutableFormProvider {
    void save(EcosFormModel model);

    void create(EcosFormModel model);

    void delete(String formId);
}
