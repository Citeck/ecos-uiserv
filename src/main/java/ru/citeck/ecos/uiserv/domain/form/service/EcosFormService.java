package ru.citeck.ecos.uiserv.domain.form.service;

import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormModel;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface EcosFormService {

    int getCount();

    void updateFormType(String formId, RecordRef typeRef);

    List<EcosFormModel> getAllForms(Predicate predicate, int max, int skip);

    List<EcosFormModel> getAllForms(int max, int skip);

    Optional<EcosFormModel> getFormByKey(String formKey);

    Optional<EcosFormModel> getFormByKey(List<String> formKeys);

    List<EcosFormModel> getFormsByKeys(List<String> formKeys);

    Optional<EcosFormModel> getFormById(String id);

    List<EcosFormModel> getFormsForExactType(RecordRef typeRef);

    List<EcosFormModel> getAllFormsForType(RecordRef typeRef);

    String save(EcosFormModel model);

    EcosFormModel getDefault();

    void delete(String id);

    void addChangeListener(Consumer<EcosFormModel> listener);
}
