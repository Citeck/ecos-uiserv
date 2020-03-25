package ru.citeck.ecos.uiserv.service.form;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.uiserv.domain.EcosFormEntity;
import ru.citeck.ecos.uiserv.repository.EcosFormsRepository;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EcosFormServiceImpl implements EcosFormService {

    private static final String DEFAULT_KEY = "DEFAULT";

    private Consumer<EcosFormModel> listener;

    private final EcosFormsRepository formsRepository;
    private final RecordsService recordsService;


    @Override
    public void addChangeListener(Consumer<EcosFormModel> listener) {
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return (int) formsRepository.count();
    }

    @Override
    public List<EcosFormModel> getAllForms(int max, int skip) {
        return formsRepository.findAll().stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Override
    public EcosFormModel getDefault() {
        return getFormByKey(DEFAULT_KEY).orElseThrow(() -> new IllegalStateException("Default form is not found!"));
    }

    @Override
    public Optional<EcosFormModel> getFormByKey(String formKey) {
        return formsRepository.findFirstByFormKey(formKey).map(this::mapToDto);
    }

    @Override
    public Optional<EcosFormModel> getFormByKey(List<String> formKeys) {

        if (CollectionUtils.isEmpty(formKeys)) {
            return Optional.empty();
        }

        return formKeys.stream()
            .map(this::getFormByKey)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }

    @Override
    public List<EcosFormModel> getFormsByKeys(List<String> formKeys) {

        if (CollectionUtils.isEmpty(formKeys)) {
            return new ArrayList<>();
        }

        return formKeys.stream()
            .distinct()
            .map(this::getFormByKey)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<EcosFormModel> getFormById(String id) {
        return formsRepository.findByExtId(id).map(this::mapToDto);
    }

    @Override
    public String save(EcosFormModel model) {

        EcosFormEntity entity = formsRepository.save(mapToEntity(model));
        EcosFormModel result = mapToDto(entity);

        listener.accept(result);

        return result.getId();
    }

    @Override
    public void delete(String id) {
        formsRepository.findByExtId(id).ifPresent(formsRepository::delete);
    }

    @Override
    public List<EcosFormModel> getAllFormsForType(RecordRef typeRef) {

        List<EcosFormEntity> forms = new ArrayList<>();
        Set<String> formIds = new HashSet<>();

        Consumer<EcosFormEntity> addIfNotAdded = form -> {
            if (formIds.add(form.getExtId())) {
                forms.add(form);
            }
        };

        try {
            ParentsAndFormByType parents = recordsService.getMeta(typeRef, ParentsAndFormByType.class);
            if (!RecordRef.isEmpty(parents.form)) {
                formsRepository.findByExtId(parents.form.getId()).ifPresent(addIfNotAdded);
            }

            formsRepository.findAllByTypeRef(typeRef.toString()).forEach(addIfNotAdded);

            if (parents.parents != null) {
                List<String> typesStr = parents.parents.stream().map(Object::toString).collect(Collectors.toList());
                formsRepository.findAllByTypeRefIn(typesStr).forEach(addIfNotAdded);
            }
        } catch (Exception e) {
            formsRepository.findAllByTypeRef(typeRef.toString()).forEach(addIfNotAdded);
            log.error("Parents forms can't be received", e);
        }

        return forms.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    private EcosFormModel mapToDto(EcosFormEntity entity) {

        EcosFormModel model = new EcosFormModel();

        model.setId(entity.getExtId());
        model.setTitle(Json.getMapper().read(entity.getTitle(), MLText.class));
        model.setDescription(Json.getMapper().read(entity.getDescription(), MLText.class));

        model.setWidth(entity.getWidth());
        model.setFormKey(entity.getFormKey());
        model.setTypeRef(RecordRef.valueOf(entity.getTypeRef()));
        model.setCustomModule(entity.getCustomModule());

        model.setI18n(Json.getMapper().read(entity.getI18n(), ObjectData.class));
        model.setAttributes(Json.getMapper().read(entity.getAttributes(), ObjectData.class));
        model.setDefinition(Json.getMapper().read(entity.getDefinition(), ObjectData.class));

        return model;
    }

    private EcosFormEntity mapToEntity(EcosFormModel model) {

        EcosFormEntity entity = null;
        if (!StringUtils.isBlank(model.getId())) {
            entity = formsRepository.findByExtId(model.getId()).orElse(null);
        }
        if (entity == null) {
            entity = new EcosFormEntity();
            if (StringUtils.isBlank(model.getId())) {
                entity.setExtId(UUID.randomUUID().toString());
            } else {
                entity.setExtId(model.getId());
            }
        }

        entity.setTitle(Json.getMapper().toString(model.getTitle()));
        entity.setWidth(model.getWidth());
        entity.setDescription(Json.getMapper().toString(model.getDescription()));
        entity.setFormKey(model.getFormKey());
        entity.setTypeRef(RecordRef.toString(model.getTypeRef()));
        entity.setCustomModule(model.getCustomModule());
        entity.setI18n(Json.getMapper().toString(model.getI18n()));
        entity.setDefinition(Json.getMapper().toString(model.getDefinition()));
        entity.setAttributes(Json.getMapper().toString(model.getAttributes()));

        return entity;
    }

    @Data
    public static class ParentsAndFormByType {
        private List<RecordRef> parents;
        private RecordRef form;
    }

    public static class FormKeys {
        private final static String ATT_FORM_KEY = "_formKey";

        @MetaAtt(ATT_FORM_KEY)
        @Getter
        @Setter
        private List<String> keys;
    }

    public static class ViewFormKeys extends FormKeys {
        private final static String ATT_VIEW_FORM_KEY = "_viewFormKey";

        @MetaAtt(ATT_VIEW_FORM_KEY)
        @Getter
        @Setter
        private List<String> viewKeys;
    }
}
