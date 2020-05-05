package ru.citeck.ecos.uiserv.journal.records.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.journal.dto.JournalColumnDto;
import ru.citeck.ecos.uiserv.journal.dto.JournalDto;
import ru.citeck.ecos.uiserv.journal.dto.legacy0.Attribute;
import ru.citeck.ecos.uiserv.journal.dto.legacy0.CreateVariant;
import ru.citeck.ecos.uiserv.journal.dto.legacy0.JournalConfig;
import ru.citeck.ecos.uiserv.journal.dto.legacy0.JournalTypeDto;
import ru.citeck.ecos.uiserv.journal.records.utils.CreateVariantDto;
import ru.citeck.ecos.uiserv.journal.records.utils.EcosTypeUtils;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JournalV0Format implements JournalModelFormat<JournalConfig> {

    private final EcosTypeUtils typeUtils;

    @Override
    public JournalConfig convert(JournalDto dto) {

        Locale currentLocale = LocaleContextHolder.getLocale();

        JournalTypeDto journalType = new JournalTypeDto();
        journalType.setId(dto.getId());
        journalType.setDatasource(dto.getSourceId());
        journalType.setGroupActions(new DataValue(Json.getMapper().newArrayNode()));

        ObjectData settings = new ObjectData(dto.getAttributes());
        settings.set("__uiserv__", true);
        journalType.setSettings(Json.getMapper().convert(settings, StrStrMap.class));

        List<Attribute> attributes = new ArrayList<>();
        for (JournalColumnDto column : dto.getColumns()) {

            Attribute att = new Attribute();
            att.setBatchEdit(new DataValue(Json.getMapper().newArrayNode()));
            att.setName(column.getName());
            att.setCriterionInvariants(new DataValue(Json.getMapper().newArrayNode()));
            att.setGroupable(column.getGroupable() != null ? column.getGroupable() : true);
            att.setIsDefault(column.getVisible() != null ? column.getVisible() : true);
            att.setSearchable(column.getSearchable() != null ? column.getSearchable() : true);
            att.setSortable(column.getSortable() != null ? column.getSortable() : true);
            att.setVisible(column.getHidden() == null || !column.getHidden());

            ObjectData attSettings = new ObjectData(column.getAttributes());
            attSettings.set("customLabel", MLText.getClosestValue(column.getLabel(), currentLocale));
            att.setSettings(Json.getMapper().convert(attSettings, StrStrMap.class));

            attributes.add(att);
        }

        journalType.setAttributes(attributes);

        JournalConfig config = new JournalConfig();
        config.setType(journalType);
        config.setNodeRef(dto.getId());
        config.setCriteria(Collections.emptyList());
        config.setPredicate(dto.getPredicate() != null ? new ObjectData(dto.getPredicate()) : null);
        config.setTitle(MLText.getClosestValue(dto.getLabel(), currentLocale));
        config.setCreateVariants(getCreateVariants(dto.getTypeRef()));

        return config;
    }

    private List<CreateVariant> getCreateVariants(RecordRef typeRef) {

        List<CreateVariantDto> variants = typeUtils.getCreateVariants(typeRef);
        Locale locale = LocaleContextHolder.getLocale();

        return variants.stream().map(cv -> {

            ObjectData attributes = cv.getAttributes();
            if (attributes == null) {
                attributes = new ObjectData();
            }

            CreateVariant variant = new CreateVariant();

            String destination = attributes.get("_parent").asText();
            if (!destination.isEmpty()) {
                variant.setDestination(destination);
            }
            if (cv.getRecordRef().getSourceId().equals("dict")) {
                variant.setType(cv.getRecordRef().getId());
            }
            variant.setFormKey(cv.getFormKey());
            variant.setFormId(RecordRef.toString(cv.getFormRef()));
            variant.setRecordRef(RecordRef.toString(cv.getRecordRef()));
            variant.setTitle(MLText.getClosestValue(cv.getName(), locale));

            variant.setCanCreate(true);

            return variant;

        }).collect(Collectors.toList());
    }

    @Override
    public int getVersion() {
        return 0;
    }

    public static class StrStrMap extends HashMap<String, String> {}
}
