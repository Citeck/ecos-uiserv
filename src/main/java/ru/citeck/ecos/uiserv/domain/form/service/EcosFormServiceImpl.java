package ru.citeck.ecos.uiserv.domain.form.service;

import kotlin.Pair;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.data.entity.EntityMeta;
import ru.citeck.ecos.commons.data.entity.EntityWithMeta;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.predicate.model.VoidPredicate;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy;
import ru.citeck.ecos.uiserv.domain.form.repo.EcosFormEntity;
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef;
import ru.citeck.ecos.uiserv.domain.form.service.provider.EcosFormsProvider;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EcosFormServiceImpl implements EcosFormService {

    private static final String VALID_ID_PATTERN_TXT = "^[\\w/.:-]+\\w$";
    private static final Pattern VALID_ID_PATTERN = Pattern.compile(VALID_ID_PATTERN_TXT);

    private static final String DEFAULT_KEY = "DEFAULT";

    private final List<BiConsumer<EntityWithMeta<EcosFormDef>, EntityWithMeta<EcosFormDef>>> listeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<EntityWithMeta<EcosFormDef>>> deleteListeners = new CopyOnWriteArrayList<>();

    private final FormsEntityDao formsEntityDao;
    private final RecordsService recordsService;

    private final Map<String, EcosFormsProvider> providers = new ConcurrentHashMap<>();

    @Override
    public void addDeleteListener(Consumer<EntityWithMeta<EcosFormDef>> listener) {
        deleteListeners.add(listener);
    }

    @Override
    public void addChangeListener(BiConsumer<EcosFormDef, EcosFormDef> listener) {
        listeners.add((before, after) -> {
            EcosFormDef beforeDef = null;
            if (before != null) {
                beforeDef = before.getEntity();
            }
            EcosFormDef afterDef = null;
            if (after != null) {
                afterDef = after.getEntity();
            }
            listener.accept(beforeDef, afterDef);
        });
    }

    @Override
    public void addChangeWithMetaListener(BiConsumer<EntityWithMeta<EcosFormDef>, EntityWithMeta<EcosFormDef>> listener) {
        listeners.add(listener);
    }

    @Override
    public int getCount() {
        return (int) formsEntityDao.count();
    }

    @Override
    public int getCount(Predicate predicate) {
        return (int) formsEntityDao.count(predicate);
    }

    @Override
    public void updateFormType(String formId, EntityRef typeRef) {

        if (EntityRef.isEmpty(typeRef)) {
            return;
        }

        EcosFormEntity form = formsEntityDao.findByExtId(formId);
        if (form != null && StringUtils.isBlank(form.getTypeRef())) {
            form.setTypeRef(typeRef.toString());
            formsEntityDao.save(form);
        }
    }

    @Override
    public List<EcosFormDef> getAllForms(Predicate predicate, int max, int skip, List<SortBy> sort) {
        return formsEntityDao.findAll(predicate, max, skip, sort).stream()
            .map(this::mapToDto)
            .map(EntityWithMeta::getEntity)
            .collect(Collectors.toList());
    }

    private List<EntityWithMeta<EcosFormDef>> getAllFormsWithMeta(
        Predicate predicate, int max, int skip, List<SortBy> sort) {
        return formsEntityDao.findAll(predicate, max, skip, sort).stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<EntityWithMeta<EcosFormDef>> getFormsByIds(List<String> ids) {

        Map<String, Integer> idsToRequestFromDb = new HashMap<>();
        List<Pair<EntityWithMeta<EcosFormDef>, Integer>> result = new ArrayList<>();
        int idx = 0;
        for (String id : ids) {
            if (id.contains("$")) {
                String providerId = id.substring(0, id.indexOf('$'));
                if (providers.containsKey(providerId)) {
                    Optional<EcosFormDef> formById = getFormById(id);
                    if (formById.isPresent()) {
                        result.add(new Pair<>(new EntityWithMeta<>(formById.get(), EntityMeta.EMPTY), idx));
                    }
                } else {
                    idsToRequestFromDb.put(id, idx);
                }
            } else {
                idsToRequestFromDb.put(id, idx);
            }
            idx++;
        }

        formsEntityDao.findAllByExtIdIn(idsToRequestFromDb.keySet()).stream()
            .map(this::mapToDto)
            .forEach(v ->
                result.add(new Pair<>(v, idsToRequestFromDb.getOrDefault(v.getEntity().getId(), 0))));

        result.sort(Comparator.comparing(Pair<EntityWithMeta<EcosFormDef>, Integer>::getSecond));

        return result.stream()
            .map(Pair::getFirst)
            .collect(Collectors.toList());
    }

    @Override
    public List<EcosFormDef> getAllForms(int max, int skip) {
        return getAllForms(VoidPredicate.INSTANCE, max, skip, Collections.emptyList());
    }

    @Override
    public List<EntityWithMeta<EcosFormDef>> getAllFormsWithMeta(int max, int skip) {
        return getAllFormsWithMeta(VoidPredicate.INSTANCE, max, skip, Collections.emptyList());
    }

    @Override
    public EcosFormDef getDefault() {
        return getFormByKey(DEFAULT_KEY).orElseThrow(() -> new IllegalStateException("Default form is not found!"));
    }

    @Override
    public Optional<EcosFormDef> getFormByKey(String formKey) {
        return Optional.ofNullable(formsEntityDao.findFirstByFormKey(formKey))
            .map(this::mapToDto)
            .map(EntityWithMeta::getEntity);
    }

    @Override
    public Optional<EcosFormDef> getFormByKey(List<String> formKeys) {

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
    public List<EcosFormDef> getFormsByKeys(List<String> formKeys) {

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
    public Optional<EcosFormDef> getFormById(String id) {
        if (StringUtils.isBlank(id)) {
            return Optional.empty();
        }
        if (id.contains("$")) {
            String resolverId = id.substring(0, id.indexOf('$'));
            EcosFormsProvider resolver = providers.get(resolverId);
            if (resolver != null) {
                return Optional.ofNullable(
                    resolver.getFormById(id.substring(resolverId.length() + 1))
                ).map(formDefWithMeta -> {
                    if (StringUtils.isBlank(formDefWithMeta.getEntity().getId())) {
                        return formDefWithMeta.getEntity().copy().withId(id).build();
                    }
                    return formDefWithMeta.getEntity();
                });
            }
        }
        return Optional.ofNullable(formsEntityDao.findByExtId(id))
            .map(this::mapToDto)
            .map(EntityWithMeta::getEntity);
    }

    @Override
    public String save(EcosFormDef model) {

        if (model.getId().contains("$")) {
            throw new RuntimeException("You can't change generated form: '" + model.getId() + "'");
        }

        EcosFormEntity entityBefore = formsEntityDao.findByExtId(model.getId());
        EntityWithMeta<EcosFormDef> formDtoBefore = null;
        if (entityBefore != null) {
            formDtoBefore = mapToDto(entityBefore);
        } else {
            if (!VALID_ID_PATTERN.matcher(model.getId()).matches()) {
                throw new IllegalArgumentException("Invalid id: '" + model.getId() + "'");
            }
        }

        EcosFormEntity entity = formsEntityDao.save(mapToEntity(model));
        EntityWithMeta<EcosFormDef> result = mapToDto(entity);

        for (BiConsumer<EntityWithMeta<EcosFormDef>, EntityWithMeta<EcosFormDef>> listener : listeners) {
            listener.accept(formDtoBefore, result);
        }

        return result.getEntity().getId();
    }

    @Override
    public void delete(String id) {
        EcosFormEntity entity = formsEntityDao.findByExtId(id);
        if (entity != null) {
            EntityWithMeta<EcosFormDef> dtoBefore = mapToDto(entity);
            formsEntityDao.delete(entity);
            for (Consumer<EntityWithMeta<EcosFormDef>> listener : deleteListeners) {
                listener.accept(dtoBefore);
            }
        }
    }

    @Override
    public List<EcosFormDef> getFormsForExactType(EntityRef typeRef) {
        List<EcosFormEntity> forms = formsEntityDao.findAllByTypeRef(typeRef.toString());
        return forms.stream()
            .map(this::mapToDto)
            .map(EntityWithMeta::getEntity)
            .collect(Collectors.toList());
    }

    @Override
    public List<EcosFormDef> getAllFormsForType(EntityRef typeRef) {

        List<EcosFormEntity> forms = new ArrayList<>();
        Set<String> formIds = new HashSet<>();

        Consumer<EcosFormEntity> addIfNotAdded = form -> {
            if (formIds.add(form.getExtId())) {
                forms.add(form);
            }
        };

        try {
            ParentsAndFormByType parents = recordsService.getAtts(typeRef, ParentsAndFormByType.class);
            if (!EntityRef.isEmpty(parents.form)) {
                Optional.ofNullable(formsEntityDao.findByExtId(parents.form.getLocalId()))
                    .ifPresent(addIfNotAdded);
            }

            formsEntityDao.findAllByTypeRef(typeRef.toString()).forEach(addIfNotAdded);

            if (parents.parents != null) {
                List<String> typesStr = parents.parents.stream().map(Object::toString).collect(Collectors.toList());
                formsEntityDao.findAllByTypeRefIn(typesStr).forEach(addIfNotAdded);
            }
        } catch (Exception e) {
            formsEntityDao.findAllByTypeRef(typeRef.toString()).forEach(addIfNotAdded);
            log.error("Parents forms can't be received", e);
        }

        return forms.stream()
            .map(this::mapToDto)
            .map(EntityWithMeta::getEntity)
            .collect(Collectors.toList());
    }

    private EntityWithMeta<EcosFormDef> mapToDto(EcosFormEntity entity) {

        EcosFormDef model = EcosFormDef.create()
            .withId(entity.getExtId())
            .withTitle(Json.getMapper().read(entity.getTitle(), MLText.class))
            .withDescription(Json.getMapper().read(entity.getDescription(), MLText.class))
            .withWidth(entity.getWidth())
            .withFormKey(entity.getFormKey())
            .withTypeRef(EntityRef.valueOf(entity.getTypeRef()))
            .withCustomModule(entity.getCustomModule())
            .withI18n(Json.getMapper().read(entity.getI18n(), ObjectData.class))
            .withAttributes(Json.getMapper().read(entity.getAttributes(), ObjectData.class))
            .withDefinition(Json.getMapper().read(entity.getDefinition(), ObjectData.class))
            .withSystem(entity.getSystem())
            .build();

        EntityMeta meta = EntityMeta.create()
            .withCreated(entity.getCreatedDate())
            .withCreator(entity.getCreatedBy())
            .withModified(entity.getLastModifiedDate())
            .withModifier(entity.getLastModifiedBy())
            .build();

        return new EntityWithMeta<>(model, meta);
    }

    private EcosFormEntity mapToEntity(EcosFormDef model) {

        EcosFormEntity entity = null;
        if (!StringUtils.isBlank(model.getId())) {
            entity = formsEntityDao.findByExtId(model.getId());
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
        entity.setTypeRef(EntityRef.toString(model.getTypeRef()));
        entity.setCustomModule(model.getCustomModule());
        entity.setI18n(Json.getMapper().toString(model.getI18n()));
        entity.setDefinition(Json.getMapper().toString(model.getDefinition()));
        entity.setAttributes(Json.getMapper().toString(model.getAttributes()));
        entity.setSystem(model.getSystem());

        return entity;
    }

    @Override
    public void register(EcosFormsProvider resolver) {
        this.providers.put(resolver.getType(), resolver);
    }

    @Data
    public static class ParentsAndFormByType {
        private List<EntityRef> parents;
        private EntityRef form;
    }

    public static class FormKeys {
        private final static String ATT_FORM_KEY = "_formKey";

        @AttName(ATT_FORM_KEY)
        @Getter
        @Setter
        private List<String> keys;
    }

    public static class ViewFormKeys extends FormKeys {
        private final static String ATT_VIEW_FORM_KEY = "_viewFormKey";

        @AttName(ATT_VIEW_FORM_KEY)
        @Getter
        @Setter
        private List<String> viewKeys;
    }
}
