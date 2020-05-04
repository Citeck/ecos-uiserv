package ru.citeck.ecos.uiserv.journal.records.utils;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EcosTypeUtils {

    private final RecordsService recordsService;

    public List<CreateVariantDto> getCreateVariants(RecordRef typeRef) {

        if (RecordRef.isEmpty(typeRef)) {
            return Collections.emptyList();
        }

        TypeCreateVariants meta = recordsService.getMeta(typeRef, TypeCreateVariants.class);

        if (meta == null) {
            return Collections.emptyList();
        }

        return meta.createVariants;
    }

    @Data
    public static class TypeCreateVariants {
        @MetaAtt("inhCreateVariants[]?json")
        private List<CreateVariantDto> createVariants;
    }
}

