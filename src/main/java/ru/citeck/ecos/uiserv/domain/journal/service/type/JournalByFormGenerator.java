package ru.citeck.ecos.uiserv.domain.journal.service.type;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalColumnDef;
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalDef;
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JournalByFormGenerator {

    public void fillData(JournalDef journal, EcosFormDef form) {
        //journal.setLabel(form.getTitle());
        //journal.setColumns(readColumns(form.getDefinition().get("components"), new ArrayList<>()));
    }

    private List<JournalColumnDef> readColumns(DataValue components, List<JournalColumnDef> result) {

        if (!components.isArray()) {
            return result;
        }

        for (DataValue component : components) {

            if (isInputComponent(component)) {
                result.add(readColumn(component));
            }

            readColumns(getInnerComponents(component), result);
        }

        return result;
    }

    private JournalColumnDef readColumn(DataValue component) {

        /*JournalColumnDef column = new JournalColumnDef();

        column.setLabel(Json.getMapper().convert(component.get("label").asText(), MLText.class));
        column.setName(component.get("key").asText());
        column.setAttribute(getAttributeOrNull(component));
        column.setEditable(!component.get("disabled").asBoolean());
        column.setAttributes(ObjectData.create(component.get("attributes")));

        String compType = component.get("type").asText();

        switch (compType) {
            case "ecosSelect":
            case "select":
                column.setType("options");
                break;
            case "selectJournal":
                column.setType("assoc");
                break;
            case "selectOrgstruct":
                column.setType("authority");
                break;
            case "textfield":
            case "textarea":
                column.setType("text");
            default:
                column.setType(compType);
                break;
        }

        ColumnControl controller = new ColumnControl();
        controller.setType(compType);
        controller.setConfig(ObjectData.create(component));
        controller.getConfig().remove("type");

        column.setControl(controller);

        return column;*/
        return null;
    }

    private String getAttributeOrNull(DataValue component) {
        String result = component.get("/properties/attribute").asText("");
        return StringUtils.isBlank(result) ? null : result;
    }

    private DataValue getInnerComponents(DataValue component) {
        if (component.get("type").asText().equals("columns")) {
            return component.get("columns");
        } else {
            return component.get("components");
        }
    }

    /**
     * Check that received component is some kind of 'input'
     */
    private boolean isInputComponent(DataValue component) {
        String type = component.get("type").asText("");
        boolean hasKey = StringUtils.isNotBlank(component.get("key").asText());
        boolean isInput = component.get("input").asBoolean();
        return isInput && hasKey && !"button".equals(type) && !"horizontalLine".equals(type);
    }
}
