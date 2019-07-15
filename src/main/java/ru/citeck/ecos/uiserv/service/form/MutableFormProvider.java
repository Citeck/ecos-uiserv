package ru.citeck.ecos.uiserv.service.form;

public interface MutableFormProvider {
    void save(EcosFormModel model);

    void create(EcosFormModel model);

    void delete(String formId);
}
