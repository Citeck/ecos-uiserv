package ru.citeck.ecos.uiserv.service.form;

import ru.citeck.ecos.records2.RecordRef;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface EcosFormService {

    int getCount();

    List<EcosFormModel> getAllForms(int max, int skip);

    Optional<EcosFormModel> getFormByKey(String formKey);

    Optional<EcosFormModel> getFormByKey(List<String> formKeys);

    Optional<EcosFormModel> getFormByKeyAndMode(String formKey, String formMode);

    List<EcosFormModel> getFormsByKeys(List<String> formKeys);

    Optional<EcosFormModel> getFormByRecord(RecordRef record, Boolean isViewMode);

    Optional<EcosFormModel> getFormById(String id);

    String save(EcosFormModel model);

    EcosFormModel getDefault();

    boolean hasForm(RecordRef record, Boolean isViewMode);

    void register(FormProvider formProvider);

    void delete(String id);

    void addChangeListener(Consumer<EcosFormModel> listener);
}
