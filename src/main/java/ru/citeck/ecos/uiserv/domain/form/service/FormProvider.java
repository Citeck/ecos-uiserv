package ru.citeck.ecos.uiserv.domain.form.service;

import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef;

import java.util.List;

public interface FormProvider {

    int getCount();

    List<EcosFormDef> getAllForms(int max, int skip);

    EcosFormDef getFormByKey(String formKey);

    EcosFormDef getFormByKeyAndMode(String formKey, String formMode);

    EcosFormDef getFormById(String id);

    int getOrder();
}
