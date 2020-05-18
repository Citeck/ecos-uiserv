package emtypes.ui.menu

import kotlin.Unit
import kotlin.jvm.functions.Function1
import org.jetbrains.annotations.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.xml.sax.SAXException
import ru.citeck.ecos.apps.module.controller.ModuleController
import ru.citeck.ecos.commons.io.file.EcosFile
import ru.citeck.ecos.commons.utils.FileUtils

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import java.util.stream.Collectors

return new ModuleController<Module, Unit>() {

    private static final Logger log = LoggerFactory.getLogger(ModuleController.class)

    @Override
    List<Module> read(@NotNull EcosFile root, Unit config) {

        return root.findFiles("**.{json,xml}")
            .stream()
            .map(this.&readModule)
            .collect(Collectors.toList())
    }

    private Module readModule(EcosFile file) {

        byte[] data = file.readAsBytes()

        try {
            Module module = new Module()
            module.setId(getMenuId(data))
            module.setFilename(file.getPath().getFileName().toString())
            module.setData(data)
            return module

        } catch (ParserConfigurationException | IOException | SAXException e) {
            log.error("Menu reading error. File: " + file.getPath(), e)
            throw new RuntimeException(e)
        }
    }

    private String getMenuId(byte[] data) throws ParserConfigurationException, IOException, SAXException {

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance()
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder()
        Document document = docBuilder.parse(new ByteArrayInputStream(data))

        return document.getElementsByTagName("id").item(0).getTextContent()
    }

    @Override
    void write(@NotNull EcosFile root, Module module, Unit config) {

        String name = FileUtils.getValidName(module.getId()) + ".xml"

        root.createFile(name, (Function1<OutputStream, Unit>) {
            OutputStream out -> out.write(module.getData())
        })
    }

    static class Module {
        String id
        String filename
        byte[] data
    }
}
