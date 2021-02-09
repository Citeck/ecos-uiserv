package ru.citeck.ecos.uiserv.domain.journal.service.format.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName;
import ru.citeck.ecos.uiserv.domain.journal.dto.CreateVariantDef;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class EcosTypeUtils {

    private final RecordsService recordsService;

    private LoadingCache<RecordRef, Optional<TypeMeta>> typesMetaCache;

    @PostConstruct
    public void init() {
        typesMetaCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build(CacheLoader.from(this::getTypeMetaImpl));
    }

    public List<RecordRef> getActions(RecordRef typeRef) {
        return getTypeMeta(typeRef)
            .map(TypeMeta::getActions)
            .orElse(Collections.emptyList());
    }

    public List<CreateVariantDef> getCreateVariants(RecordRef typeRef) {
        return getTypeMeta(typeRef)
            .map(TypeMeta::getCreateVariants)
            .orElse(Collections.emptyList());
    }

    private Optional<TypeMeta> getTypeMeta(RecordRef typeRef) {
        if (RecordRef.isEmpty(typeRef)) {
            return Optional.empty();
        }
        return typesMetaCache.getUnchecked(typeRef);
    }

    private Optional<TypeMeta> getTypeMetaImpl(RecordRef typeRef) {
        // Do not remove Optional. Interface can be changed with @Nullable
        return Optional.ofNullable(recordsService.getMeta(typeRef, TypeMeta.class));
    }

    @Data
    public static class TypeMeta {
        @AttName("inhCreateVariants[]?json")
        private List<CreateVariantDef> createVariants;
        @AttName("_actions[]?id")
        private List<RecordRef> actions;
    }
}

