package emtypes.ui.menu

import kotlin.Unit
import kotlin.jvm.functions.Function1
import org.jetbrains.annotations.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.xml.sax.SAXException
import ru.citeck.ecos.apps.module.controller.ModuleController
import ru.citeck.ecos.apps.module.controller.ModuleMeta
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.io.file.EcosFile
import ru.citeck.ecos.commons.utils.FileUtils
import ru.citeck.ecos.records2.graphql.meta.value.EmptyValue

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import java.util.stream.Collectors

return new ModuleController<Module, Unit>() {

    private static final Logger log = LoggerFactory.getLogger(ModuleController.class)

    @Override
    List<Module> read(@NotNull EcosFile root, Unit config) {

        return root.findFiles("**.xml")
            .stream()
            .map(this.&readModule)
            .collect(Collectors.toList())
    }

    private Module readModule(EcosFile file) {

        byte[] data = file.readAsBytes()

        try {

            Module module = new Module()
            module.setId(getMenuId(data))
            module.setXmlData(data)
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
            OutputStream out -> out.write(module.getXmlData())
        })
    }

    @Override
    Map<String, Object> getMetaValueMixinAttributes(Module module, Unit config) {
        return Collections.emptyMap()
    }

    @Override
    Object getAsMetaValue(Module module, Unit config) {
        return EmptyValue
    }

    @Override
    Module mutate(Module module, @NotNull ObjectData data, Unit config) {
        return module
    }

    @Override
    Module merge(Module oldModule, Module newModule, Unit config) {
        return newModule
    }

    @Override
    ModuleMeta getModuleMeta(Module module, Unit config) {
        return new ModuleMeta(module.id, module.id, [])
    }

    static class Module {
        String id
        byte[] xmlData
    }
}
