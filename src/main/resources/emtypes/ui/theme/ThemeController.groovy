package emtypes.ui.theme

import kotlin.Unit
import org.jetbrains.annotations.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.xml.sax.SAXException
import ru.citeck.ecos.apps.module.controller.ModuleController
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.io.file.EcosFile
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.RecordRef

import javax.xml.parsers.ParserConfigurationException
import java.util.stream.Collectors

return new ModuleController<Module, Unit>() {

    private static final Logger log = LoggerFactory.getLogger(ModuleController.class)

    @Override
    List<Module> read(@NotNull EcosFile root, Unit config) {

        return root.findFiles("**/meta.{json,yml}")
            .stream()
            .map(this.&readModule)
            .collect(Collectors.toList())
    }

    private Module readModule(EcosFile metaFile) {

        try {

            Module module = new Module()
            module.id = metaFile.parent.name
            module.meta = Json.getMapper().read(metaFile, ModuleMeta.class)
            module.styles = new HashMap<>()

            for (EcosFile file : metaFile.parent.findFiles("**.css")) {
                module.styles.put(file.name.substring(0, file.name.length() - 4), file.readAsBytes())
            }

            return module

        } catch (ParserConfigurationException | IOException | SAXException e) {
            log.error("Theme reading error. File: " + metaFile.getPath(), e)
            throw new RuntimeException(e)
        }
    }

    @Override
    void write(@NotNull EcosFile root, Module module, Unit config) {

        def dir = root.createDir(module.id)
        if (module.styles != null) {
            for (Map.Entry<String, byte[]> entry : module.styles) {
                dir.createFile(entry.getKey() + ".css", entry.getValue())
            }
        }
        dir.createFile("meta.json", Json.getMapper().toBytes(module.meta));
    }

    static class ModuleMeta {
        MLText name
        Map<String, RecordRef> images = new HashMap<>()
    }

    static class Module {
        String id;
        ModuleMeta meta
        Map<String, byte[]> styles = new HashMap<>()
    }
}
