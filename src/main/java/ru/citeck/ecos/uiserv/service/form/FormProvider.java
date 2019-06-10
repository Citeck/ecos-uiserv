package ru.citeck.ecos.uiserv.service.form;

import ru.citeck.ecos.uiserv.domain.EcosFormModel;

public interface FormProvider {

    EcosFormModel getFormByKey(String formKey);

    EcosFormModel getFormById(String id);

    int getOrder();
}
