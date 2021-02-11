package ru.citeck.ecos.uiserv.domain.journal.service.format;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.uiserv.domain.journal.service.format.util.EcosTypeUtils;

@Component
@RequiredArgsConstructor
public class JournalV0Format {

    private final EcosTypeUtils typeUtils;

    /*@Override
    public JournalConfig convert(JournalWithMeta dto) {

        Locale currentLocale = LocaleContextHolder.getLocale();

        JournalTypeDto journalType = new JournalTypeDto();
        journalType.setId(dto.getId());
        journalType.setDatasource(dto.getSourceId());
        journalType.setGroupActions(DataValue.create(Json.getMapper().newArrayNode()));

        ObjectData settings = ObjectData.create(dto.getAttributes());
        settings.set("__uiserv__", true);
        journalType.setSettings(Json.getMapper().convert(settings, StrStrMap.class));

        List<Attribute> attributes = new ArrayList<>();
        for (JournalColumnDef column : dto.getColumns()) {

            Attribute att = new Attribute();
            att.setBatchEdit(DataValue.create(Json.getMapper().newArrayNode()));
            att.setName(column.getName());
            att.setCriterionInvariants(DataValue.create(Json.getMapper().newArrayNode()));
            att.setGroupable(column.getGroupable() != null ? column.getGroupable() : true);
            att.setIsDefault(column.getVisible() != null ? column.getVisible() : true);
            att.setSearchable(column.getSearchable() != null ? column.getSearchable() : true);
            att.setSortable(column.getSortable() != null ? column.getSortable() : true);
            att.setVisible(column.getHidden() == null || !column.getHidden());

            ObjectData attSettings = ObjectData.create(column.getAttributes());
            attSettings.set("customLabel", MLText.getClosestValue(column.getLabel(), currentLocale));
            att.setSettings(Json.getMapper().convert(attSettings, StrStrMap.class));

            attributes.add(att);
        }

        journalType.setAttributes(attributes);

        JournalConfig config = new JournalConfig();
        config.setType(journalType);
        config.setNodeRef(dto.getId());
        config.setCriteria(Collections.emptyList());
        config.setPredicate(dto.getPredicate() != null ? ObjectData.create(dto.getPredicate()) : null);
        config.setTitle(MLText.getClosestValue(dto.getLabel(), currentLocale));
        config.setCreateVariants(getCreateVariants(dto));

        return config;
    }

    private List<CreateVariant> getCreateVariants(JournalWithMeta dto) {

        List<CreateVariantDef> typeVariants = typeUtils.getCreateVariants(dto.getTypeRef());
        List<CreateVariantDef> journalVariants = dto.getCreateVariants();
        List<CreateVariantDef> variants = Stream.concat(typeVariants.stream(), journalVariants.stream())
            .collect(Collectors.toList());

        Locale locale = LocaleContextHolder.getLocale();

        return variants.stream().map(cv -> {

            ObjectData attributes = cv.getAttributes();
            if (attributes == null) {
                attributes = ObjectData.create();
            }

            CreateVariant variant = new CreateVariant();

            String destination = attributes.get("_parent").asText();
            if (!destination.isEmpty()) {
                variant.setDestination(destination);
            }
            if (cv.getRecordRef().getSourceId().equals("dict")) {
                variant.setType(cv.getRecordRef().getId());
            }
            variant.setFormId(RecordRef.toString(cv.getFormRef()));
            variant.setRecordRef(RecordRef.toString(cv.getRecordRef()));
            variant.setTitle(MLText.getClosestValue(cv.getName(), locale));
            variant.setCanCreate(true);
            variant.setAttributes(cv.getAttributes());

            return variant;

        }).collect(Collectors.toList());
    }

    @Override
    public int getVersion() {
        return 0;
    }

    public static class StrStrMap extends HashMap<String, String> {}*/

}
