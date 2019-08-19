package ru.citeck.ecos.uiserv.service.form;

public interface FormProvider {

    EcosFormModel getFormByKey(String formKey);

    EcosFormModel getFormById(String id);

    int getOrder();
}
