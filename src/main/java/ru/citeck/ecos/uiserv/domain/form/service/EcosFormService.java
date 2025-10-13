package ru.citeck.ecos.uiserv.domain.form.service;

import ru.citeck.ecos.commons.data.entity.EntityWithMeta;
import ru.citeck.ecos.model.lib.workspace.IdInWs;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy;
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef;
import ru.citeck.ecos.uiserv.domain.form.service.provider.EcosFormsProvider;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface EcosFormService {

    int getCount();

    int getCount(Predicate predicate);

    void updateFormType(IdInWs formId, EntityRef typeRef);

    List<EcosFormDef> getAllForms(Predicate predicate, List<String> workspaces, int max, int skip, List<SortBy> sort);

    List<EcosFormDef> getAllForms(int max, int skip);

    List<EntityWithMeta<EcosFormDef>> getFormsByIds(List<IdInWs> ids);

    List<EntityWithMeta<EcosFormDef>> getAllFormsWithMeta(int max, int skip);

    Optional<EcosFormDef> getFormByKey(String formKey);

    Optional<EcosFormDef> getFormByKey(List<String> formKeys);

    List<EcosFormDef> getFormsByKeys(List<String> formKeys);

    Optional<EcosFormDef> getFormById(IdInWs id);

    Optional<EntityWithMeta<EcosFormDef>> getFormByIdWithMeta(IdInWs id);

    List<EcosFormDef> getFormsForExactType(EntityRef typeRef);

    List<EcosFormDef> getAllFormsForType(EntityRef typeRef);

    EcosFormDef save(EcosFormDef model);

    EcosFormDef getDefault();

    void delete(IdInWs id);

    void addDeleteListener(Consumer<EntityWithMeta<EcosFormDef>> listener);

    void addChangeListener(BiConsumer<EcosFormDef, EcosFormDef> listener);

    void addChangeWithMetaListener(BiConsumer<EntityWithMeta<EcosFormDef>, EntityWithMeta<EcosFormDef>> listener);

    void register(EcosFormsProvider resolver);
}
