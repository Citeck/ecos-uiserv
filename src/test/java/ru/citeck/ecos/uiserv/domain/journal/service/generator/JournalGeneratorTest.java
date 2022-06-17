package ru.citeck.ecos.uiserv.domain.journal.service.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.journal.service.type.JournalByFormGenerator;
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@ExtendWith(EcosSpringExtension.class)
@SpringBootTest(classes = Application.class)
public class JournalGeneratorTest {

    private static final String TEST_FORM_PATH = "src/test/resources/form/ecos-form.json";

    private JournalByFormGenerator journalGenerator;

    @BeforeEach
    void setUp() {
        journalGenerator = new JournalByFormGenerator();
    }

    @Test
    void readForm() throws IOException {

        //  arrange

/*        JournalDef journalDef = new JournalDef();
        journalDef.setId("JOURNAL_ECOS_FORM");
        journalDef.setLabel(new MLText("Form ui"));

        JournalColumnDef widthColumn = new JournalColumnDef();
        widthColumn.setName("width");
        widthColumn.setLabel(new MLText("Width"));
        widthColumn.setEditable(true);
        widthColumn.setVisible(true);
        widthColumn.setAttributes(ObjectData.create("{}"));*/

        /*ColumnEditorDto widthEditor = new ColumnEditorDto();
        widthEditor.setType("ecosSelect");
        widthEditor.setConfig(ObjectData.create(
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
        widthOptions.setConfig(ObjectData.create(
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

        JournalDto resultDto = journalGenerator.getJournalImpl(formContent);

        //  assert

        Assert.assertEquals(journalDto, resultDto);*/
    }

    private String readFormFileContent() throws IOException {
        File file = new File(TEST_FORM_PATH);
        String absolutePath = file.getAbsolutePath();
        return new String(Files.readAllBytes(Paths.get(absolutePath)));
    }
}
