package ru.citeck.ecos.uiserv.service.form;

public interface FormProvider {

    EcosFormModel getFormByKey(String formKey);

    EcosFormModel getFormByKeyAndMode(String formKey, String formMode);

    EcosFormModel getFormById(String id);

    int getOrder();
}
