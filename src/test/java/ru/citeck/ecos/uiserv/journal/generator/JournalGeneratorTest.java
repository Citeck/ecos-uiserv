package ru.citeck.ecos.uiserv.journal.generator;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.uiserv.journal.dto.ColumnEditorDto;
import ru.citeck.ecos.uiserv.journal.dto.JournalColumnDto;
import ru.citeck.ecos.uiserv.journal.dto.JournalDto;
import ru.citeck.ecos.uiserv.journal.dto.JournalOptionsDto;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(SpringExtension.class)
public class JournalGeneratorTest {

    private static final String TEST_FORM_PATH = "src/test/resources/form/ecos-form.json";

    private JournalGenerator journalGenerator;

    @BeforeEach
    void setUp() {
        journalGenerator = new JournalGenerator();
    }

    @Test
    void readForm() throws IOException {

        //  arrange

        JournalDto journalDto = new JournalDto();
        journalDto.setId("JOURNAL_ECOS_FORM");
        journalDto.setLabel(new MLText("Form ui"));

        JournalColumnDto widthColumn = new JournalColumnDto();
        widthColumn.setName("width");
        widthColumn.setLabel(new MLText("Width"));
        widthColumn.setEditable(true);
        widthColumn.setVisible(true);
        widthColumn.setAttributes(new ObjectData("{}"));

        ColumnEditorDto widthEditor = new ColumnEditorDto();
        widthEditor.setType("ecosSelect");
        widthEditor.setConfig(new ObjectData(
            "   {" +
                "                \"label\": \"Width\",\n" +
                "                \"dataSrc\": \"values\",\n" +
                "                \"data\": {\n" +
                "                    \"values\": [\n" +
                "                        {\n" +
                "                            \"label\": \"Default\",\n" +
                "                            \"value\": \"default\"\n" +
                "                        },\n" +
                "                        {\n" +
                "                            \"label\": \"Large\",\n" +
                "                            \"value\": \"lg\"\n" +
                "                        },\n" +
                "                        {\n" +
                "                            \"label\": \"Extra Large\",\n" +
                "                            \"value\": \"extra-lg\"\n" +
                "                        }\n" +
                "                    ],\n" +
                "                    \"json\": \"\",\n" +
                "                    \"url\": \"\",\n" +
                "                    \"resource\": \"\",\n" +
                "                    \"custom\": \"\"\n" +
                "                },\n" +
                "                \"key\": \"width\",\n" +
                "                \"attributes\": {},\n" +
                "                \"input\": true,\n" +
                "                \"hidden\": false,\n" +
                "                \"disabled\": false " +
                "   }"
        ));
        widthColumn.setEditor(widthEditor);

        JournalOptionsDto widthOptions = new JournalOptionsDto();
        widthOptions.setType("values");
        widthOptions.setConfig(new ObjectData(
            "{\n" +
                "                    \"values\": [\n" +
                "                        {\n" +
                "                            \"label\": \"Default\",\n" +
                "                            \"value\": \"default\"\n" +
                "                        },\n" +
                "                        {\n" +
                "                            \"label\": \"Large\",\n" +
                "                            \"value\": \"lg\"\n" +
                "                        },\n" +
                "                        {\n" +
                "                            \"label\": \"Extra Large\",\n" +
                "                            \"value\": \"extra-lg\"\n" +
                "                        }\n" +
                "                    ],\n" +
                "                    \"json\": \"\",\n" +
                "                    \"url\": \"\",\n" +
                "                    \"resource\": \"\",\n" +
                "                    \"custom\": \"\"\n" +
                "                }"
        ));
        widthColumn.setOptions(widthOptions);

        List<JournalColumnDto> columns = new ArrayList<>();
        columns.add(widthColumn);
        journalDto.setColumns(columns);

        String formContent = this.readFormFileContent();

        //  act

        JournalDto resultDto = journalGenerator.readForm(formContent);

        //  assert

        Assert.assertEquals(journalDto, resultDto);
    }

    private String readFormFileContent() throws IOException {
        File file = new File(TEST_FORM_PATH);
        String absolutePath = file.getAbsolutePath();
        return new String(Files.readAllBytes(Paths.get(absolutePath)));
    }
}
