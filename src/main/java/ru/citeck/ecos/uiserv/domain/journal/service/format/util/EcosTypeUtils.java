package ru.citeck.ecos.uiserv.domain.journal.service.format.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.model.lib.type.dto.CreateVariantDef;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;

import jakarta.annotation.PostConstruct;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class EcosTypeUtils {

    private final RecordsService recordsService;

    private LoadingCache<EntityRef, Optional<TypeMeta>> typesMetaCache;

    @PostConstruct
    public void init() {
        typesMetaCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build(CacheLoader.from(this::getTypeMetaImpl));
    }

    public List<EntityRef> getActions(EntityRef typeRef) {
        return getTypeMeta(typeRef)
            .map(TypeMeta::getActions)
            .orElse(Collections.emptyList());
    }

    public List<CreateVariantDef> getCreateVariants(EntityRef typeRef) {
        return getTypeMeta(typeRef)
            .map(TypeMeta::getCreateVariants)
            .orElse(Collections.emptyList());
    }

    private Optional<TypeMeta> getTypeMeta(EntityRef typeRef) {
        if (EntityRef.isEmpty(typeRef)) {
            return Optional.empty();
        }
        return typesMetaCache.getUnchecked(typeRef);
    }

    private Optional<TypeMeta> getTypeMetaImpl(EntityRef typeRef) {
        return Optional.of(recordsService.getAtts(typeRef, TypeMeta.class));
    }

    @Data
    public static class TypeMeta {
        @AttName("inhCreateVariants[]?json")
        private List<CreateVariantDef> createVariants;
        @AttName("_actions[]?id")
        private List<EntityRef> actions;
    }
}

