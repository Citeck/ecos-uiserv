package ru.citeck.ecos.uiserv.domain.journal.service.format;

public class JournalV1Format {
/*
    private final EcosTypeUtils typeUtils;

    @Override
    public JournalConfigResp convert(JournalWithMeta dto) {

        JournalConfigResp resp = new JournalConfigResp();
        resp.setId(dto.getId());
        resp.setParams(Json.getMapper().convert(dto.getAttributes(), StrStrMap.class));

        if (resp.getParams() == null) {
            resp.setParams(new HashMap<>());
        }

        if (dto.getSortBy() != null && dto.getSortBy().size() > 0) {
            String sortValue = Json.getMapper().toString(dto.getSortBy().stream().map(sort -> {
                ObjectData targetSort = ObjectData.create();
                targetSort.set("id", sort.getAttribute());
                targetSort.set("order", sort.isAscending() ? "asc": "desc");
                return targetSort;
            }).collect(Collectors.toList()));
            resp.getParams().put("defaultSortBy", sortValue);
        }


        if (Boolean.FALSE.equals(dto.getEditable())) {
            resp.getParams().put("disableTableEditing", "true");
        }

        resp.setSourceId(dto.getSourceId());
        resp.setComputed(dto.getComputed());

        JournalMeta meta = new JournalMeta();
        meta.setNodeRef(dto.getId());
        if (dto.getActions() != null) {
            meta.setActions(dto.getActions());
        } else {
            meta.setActions(typeUtils.getActions(dto.getTypeRef()));
        }
        meta.setGroupBy(DataValue.create(dto.getGroupBy()));
        if (dto.getMetaRecord() != null) {
            meta.setMetaRecord(dto.getMetaRecord().toString());
        }
        meta.setPredicate(DataValue.create(dto.getPredicate()));
        if (meta.getPredicate().isNull()) {
            meta.setPredicate(DataValue.createObj());
        }
        if (dto.getLabel() != null) {
            meta.setTitle(dto.getLabel().getClosestValue(LocaleContextHolder.getLocale()));
        }

        if (dto.getCreateVariants() != null && !dto.getCreateVariants().isEmpty()) {

            meta.setCreateVariants(dto.getCreateVariants().stream().map(variant -> {
                CreateVariant variantV1 = new CreateVariant();
                variantV1.setAttributes(variant.getAttributes().getData().asMap(String.class, String.class));
                variantV1.setCanCreate(true);
                variantV1.setFormId(variant.getFormRef().toString());
                variantV1.setRecordRef(variant.getRecordRef().toString());
                variantV1.setTitle(MLText.getClosestValue(variant.getName(), QueryContext.getCurrent().getLocale()));
                return variantV1;
            }).collect(Collectors.toList()));
        } else {
            meta.setCreateVariants(getCreateVariants(dto.getTypeRef()));
        }

        meta.setGroupActions(dto.getGroupActions());
        resp.setMeta(meta);

        List<Column> columns = new ArrayList<>();

        if (dto.getColumns() != null) {

            for (JournalColumnDef column : dto.getColumns()) {

                Column respColumn = new Column();
                respColumn.setMultiple(column.getMultiple());
                respColumn.setNewFormatter(column.getFormatter());
                respColumn.setNewEditor(column.getEditor());
                respColumn.setComputed(column.getComputed());
                respColumn.setInnerSchema(column.getInnerSchema());
                respColumn.setHidden(Boolean.TRUE.equals(column.getHidden()));

                if (column.getLabel() != null) {
                    respColumn.setText(column.getLabel().getClosestValue(LocaleContextHolder.getLocale()));
                } else {
                    respColumn.setText(column.getName());
                }
                respColumn.setAttribute(column.getName());
                respColumn.setDefault(!Boolean.FALSE.equals(column.getVisible()));

                respColumn.setGroupable(Boolean.TRUE.equals(column.getGroupable()));
                respColumn.setParams(Json.getMapper().convert(column.getAttributes(), StrStrMap.class));
                if (respColumn.getParams() == null) {
                    respColumn.setParams(new HashMap<>());
                }
                respColumn.setSchema(column.getAttribute());
                respColumn.setSearchable(Boolean.TRUE.equals(column.getSearchable()));
                respColumn.setSortable(Boolean.TRUE.equals(column.getSortable()));
                respColumn.setType(column.getType());
                respColumn.setVisible(!Boolean.TRUE.equals(column.getHidden()));
                respColumn.setEditable(!Boolean.FALSE.equals(column.getEditable()));

                ColumnControl control = column.getControl();

                if (control != null) {

                    Formatter respFormatter = new Formatter();
                    respFormatter.setName(control.getType());
                    respFormatter.setParams(control.getConfig());
                    respColumn.setFormatter(respFormatter);

                    String type = control.getType();

                    if ("selectJournal".equals(type)) {
                        String journalId = control.getConfig().get("journalId").asText();
                        if (!journalId.isEmpty()) {
                            respColumn.setEditorKey(journalId);
                        }
                    }
                }

                columns.add(respColumn);
            }
        }
        resp.setColumns(columns);

        return resp;
    }

    private List<CreateVariant> getCreateVariants(RecordRef typeRef) {

        List<CreateVariantDef> variants = typeUtils.getCreateVariants(typeRef);
        Locale locale = LocaleContextHolder.getLocale();

        List<CreateVariant> result = new ArrayList<>();
        for (CreateVariantDef variant : variants) {

            CreateVariant resultVariant = new CreateVariant();
            resultVariant.setAttributes(Json.getMapper().convert(variant.getAttributes(), StrStrMap.class));
            resultVariant.setCanCreate(true);

            ObjectData attributes = variant.getAttributes();

            String destination = attributes.get("_parent").asText();
            if (!destination.isEmpty()) {
                resultVariant.setDestination(destination);
            }
            resultVariant.setFormId(RecordRef.toString(variant.getFormRef()));
            resultVariant.setTitle(MLText.getClosestValue(variant.getName(), locale));

            resultVariant.setRecordRef(RecordRef.toString(variant.getRecordRef()));

            if (variant.getRecordRef().getSourceId().equals("dict")) {
                resultVariant.setType(variant.getRecordRef().getId());
            }

            result.add(resultVariant);
        }

        return result;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    public static class StrStrMap extends HashMap<String, String> {}*/
}
