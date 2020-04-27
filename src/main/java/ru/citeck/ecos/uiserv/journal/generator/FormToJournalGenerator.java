package ru.citeck.ecos.uiserv.journal.generator;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import ecos.com.fasterxml.jackson210.databind.node.ArrayNode;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.uiserv.journal.dto.ColumnEditorDto;
import ru.citeck.ecos.uiserv.journal.dto.JournalColumnDto;
import ru.citeck.ecos.uiserv.journal.dto.JournalDto;
import ru.citeck.ecos.uiserv.journal.dto.JournalOptionsDto;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class FormToJournalGenerator {

    private static final String JOURNAL_PREFIX = "JOURNAL_";
    private static final String JOURNAL_TEXT = "journal";

    public JournalDto read(String content) {
        JsonNode node = Json.getMapper().read(content);
        if (node == null) {
            throw new RuntimeException("Cannot get Journal from form. Cannot read JSON: " + content);
        }

        JournalDto journalDto = new JournalDto();

        JsonNode idNode = node.path("id");
        if (!idNode.isMissingNode()) {
            journalDto.setId(JOURNAL_PREFIX + idNode.asText());
        } else {
            JsonNode formKeyNode = node.path("formKey");
            if (!formKeyNode.isMissingNode()) {
                journalDto.setId(JOURNAL_PREFIX + formKeyNode.asText());
            } else {
                throw new RuntimeException("Cannot get Journal from form. Cannot create 'id' for journal.");
            }
        }

        JsonNode titleNode = node.path("title");
        if (!titleNode.isMissingNode() && !titleNode.isNull()) {
            journalDto.setLabel(new MLText(titleNode.asText() + " " + JOURNAL_TEXT));
        }

        ArrayNode components = node
            .path("definition")
            .withArray("components");
        if (!components.isMissingNode() && !components.isMissingNode()) {
            List<JournalColumnDto> columnsDtos = this.readColumns(components);
            journalDto.setColumns(columnsDtos);
        }

        return journalDto;
    }

    private List<JournalColumnDto> readColumns(@NonNull ArrayNode componentsNode) {

        List<JournalColumnDto> columnsDtos = new ArrayList<>();

        for (JsonNode componentJsonNode : componentsNode) {

            if (!isInputFormElement(componentJsonNode) || isButton(componentJsonNode)) {
                continue;
            }

            if (hasInnerComponents(componentJsonNode)) {

                ArrayNode innerColumnsNodes = componentJsonNode.withArray("columns");
                for (JsonNode columnNode : innerColumnsNodes) {

                    ArrayNode innerComponentsNodes = columnNode.withArray("components");
                    if (!innerComponentsNodes.isMissingNode() && !innerComponentsNodes.isNull()) {
                        columnsDtos.addAll(this.readColumns(innerComponentsNodes));
                    }
                }
                continue;
            }

            JournalColumnDto columnDto = this.readColumn(componentJsonNode);
            columnsDtos.add(columnDto);
        }

        return columnsDtos;
    }

    private JournalColumnDto readColumn(JsonNode componentNode) {

        JournalColumnDto columnDto = new JournalColumnDto();

        JsonNode labelNode = componentNode.path("label");
        if (!labelNode.isMissingNode() && !labelNode.isNull()) {
            columnDto.setLabel(new MLText(labelNode.asText()));
        }

        JsonNode keyNode = componentNode.path("key");
        if (!keyNode.isMissingNode() && !keyNode.isNull()) {
            columnDto.setName(keyNode.asText());
        }

        JsonNode attributeNode = componentNode.path("properties").path("attribute");
        if (!attributeNode.isMissingNode() && !attributeNode.isNull()) {
            columnDto.setAttribute(attributeNode.asText());
        }

        JsonNode protectedNode = componentNode.path("disabled");
        if (!protectedNode.isMissingNode() && !protectedNode.isNull()) {
            columnDto.setEditable(!protectedNode.asBoolean());
        }

        JsonNode hiddenNode = componentNode.path("hidden");
        if (!hiddenNode.isMissingNode() && !hiddenNode.isNull()) {
            columnDto.setVisible(!hiddenNode.asBoolean());
        }

        JsonNode attributesNode = componentNode.path("attributes");
        if (!attributesNode.isMissingNode() && !attributesNode.isNull()) {
            columnDto.setAttributes(new ObjectData(attributesNode));
        }

        JsonNode dataSrcNode = componentNode.path("dataSrc");
        if (!dataSrcNode.isMissingNode() && !dataSrcNode.isNull()) {

            JournalOptionsDto optionsDto = new JournalOptionsDto();
            optionsDto.setType(dataSrcNode.asText());

            JsonNode dataNode = componentNode.path("data");
            if (!dataNode.isMissingNode() && !dataNode.isNull()) {
                optionsDto.setConfig(new ObjectData(dataNode));
            }

            columnDto.setOptions(optionsDto);
        }

        JsonNode typeNode = componentNode.path("type");
        if (!typeNode.isMissingNode() && !typeNode.isNull()) {

            ColumnEditorDto editorDto = new ColumnEditorDto();
            editorDto.setType(typeNode.asText());

            ObjectData componentData = new ObjectData(componentNode);
            componentData.remove("type");
            editorDto.setConfig(componentData);

            columnDto.setEditor(editorDto);
        }

        return columnDto;
    }

    /**
     * Check that received JsonNode has components inside
     *
     * @param componentNode
     * @return boolean value of checking result
     */
    private boolean hasInnerComponents(JsonNode componentNode) {
        return componentNode.get("columns") != null;
    }

    /**
     * Check that received JsonNode has type "button"
     *
     * @param componentNode
     * @return boolean value of checking result
     */
    private boolean isButton(JsonNode componentNode) {
        return componentNode.path("type").asText().equals("button");
    }

    /**
     * Check that received JsonNode is some kind of 'input'
     *
     * @param componentNode
     * @return boolean value of checking result
     */
    private boolean isInputFormElement(JsonNode componentNode) {
        return componentNode.path("input").asBoolean();
    }
}
