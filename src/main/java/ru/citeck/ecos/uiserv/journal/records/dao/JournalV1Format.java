package ru.citeck.ecos.uiserv.journal.records.dao;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.uiserv.journal.dto.ColumnEditorDto;
import ru.citeck.ecos.uiserv.journal.dto.ColumnFormatterDto;
import ru.citeck.ecos.uiserv.journal.dto.JournalColumnDto;
import ru.citeck.ecos.uiserv.journal.dto.JournalDto;
import ru.citeck.ecos.uiserv.journal.dto.legacy1.Column;
import ru.citeck.ecos.uiserv.journal.dto.legacy1.Formatter;
import ru.citeck.ecos.uiserv.journal.dto.legacy1.JournalConfigResp;
import ru.citeck.ecos.uiserv.journal.dto.legacy1.JournalMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
public class JournalV1Format implements JournalModelFormat<JournalConfigResp> {

    @Override
    public JournalConfigResp convert(JournalDto dto) {

        JournalConfigResp resp = new JournalConfigResp();
        resp.setId(dto.getId());
        resp.setParams(Json.getMapper().convert(dto.getAttributes(), StrStrMap.class));
        resp.setSourceId(dto.getSourceId());

        JournalMeta meta = new JournalMeta();
        meta.setActions(dto.getActions());
        meta.setGroupBy(Json.getMapper().convert(dto.getGroupBy(), JsonNode.class));
        if (dto.getMetaRecord() != null) {
            meta.setMetaRecord(dto.getMetaRecord().toString());
        }
        meta.setPredicate(Json.getMapper().convert(dto.getPredicate(), JsonNode.class));
        if (dto.getLabel() != null) {
            meta.setTitle(dto.getLabel().getClosestValue(LocaleContextHolder.getLocale()));
        }
        resp.setMeta(meta);

        List<Column> columns = new ArrayList<>();

        if (dto.getColumns() != null) {

            for (JournalColumnDto column : dto.getColumns()) {

                Column respColumn = new Column();

                if (column.getLabel() != null) {
                    respColumn.setText(column.getLabel().getClosestValue(LocaleContextHolder.getLocale()));
                } else {
                    respColumn.setText(column.getName());
                }
                respColumn.setAttribute(column.getName());
                respColumn.setDefault(!Boolean.FALSE.equals(column.getVisible()));

                respColumn.setGroupable(Boolean.TRUE.equals(column.getGroupable()));
                respColumn.setParams(Json.getMapper().convert(column.getAttributes(), StrStrMap.class));
                respColumn.setSchema(column.getAttribute());
                respColumn.setSearchable(Boolean.TRUE.equals(column.getSearchable()));
                respColumn.setSortable(Boolean.TRUE.equals(column.getSortable()));
                respColumn.setType(column.getType());
                respColumn.setVisible(!Boolean.TRUE.equals(column.getHidden()));

                ColumnFormatterDto formatter = column.getFormatter();
                if (formatter != null) {
                    Formatter respFormatter = new Formatter();
                    respFormatter.setName(formatter.getType());
                    respFormatter.setParams(formatter.getConfig());
                    respColumn.setFormatter(respFormatter);
                }

                ColumnEditorDto editor = column.getEditor();
                if (editor != null && editor.getConfig() != null) {
                    String journalId = editor.getConfig().get("journalId").asText();
                    if (StringUtils.isNotBlank(journalId)) {
                        respColumn.setEditorKey(journalId);
                    }
                }

                columns.add(respColumn);
            }
        }
        resp.setColumns(columns);

        return resp;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    public static class StrStrMap extends HashMap<String, String> {}
}
