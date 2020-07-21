package ru.citeck.ecos.uiserv.domain.form.service;

import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormModel;

import java.util.List;

public interface FormProvider {

    int getCount();

    List<EcosFormModel> getAllForms(int max, int skip);

    EcosFormModel getFormByKey(String formKey);

    EcosFormModel getFormByKeyAndMode(String formKey, String formMode);

    EcosFormModel getFormById(String id);

    int getOrder();
}
