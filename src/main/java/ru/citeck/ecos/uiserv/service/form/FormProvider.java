package ru.citeck.ecos.uiserv.service.form;

import java.util.List;

public interface FormProvider {

    int getCount();

    List<EcosFormModel> getAllForms(int max, int skip);

    EcosFormModel getFormByKey(String formKey);

    EcosFormModel getFormByKeyAndMode(String formKey, String formMode);

    EcosFormModel getFormById(String id);

    int getOrder();
}
