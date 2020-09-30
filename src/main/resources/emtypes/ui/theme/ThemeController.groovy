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

import javax.xml.parsers.ParserConfigurationException
import java.nio.file.Path
import java.util.stream.Collectors

return new ModuleController<Module, Unit>() {

    // ThemeService should has the same constants
    public static final List<String> META_EXTENSIONS = Arrays.asList("yml", "yaml", "json");
    public static final List<String> RES_EXTENSIONS = Arrays.asList("png", "jpeg", "jpg", "svg", "css");

    private static final Logger log = LoggerFactory.getLogger(ModuleController.class)

    @Override
    List<Module> read(@NotNull EcosFile root, Unit config) {

        return root.findFiles("**/meta.{" + META_EXTENSIONS.join(",") + "}")
            .stream()
            .map(this.&readModule)
            .collect(Collectors.toList())
    }

    private Module readModule(EcosFile metaFile) {

        try {

            Module module = new Module()
            module.id = metaFile.parent.name
            module.meta = Json.getMapper().read(metaFile, ModuleMeta.class)

            Path metaParentPath = metaFile.parent.getPath()
            module.resources = new HashMap<>()

            for (EcosFile file : metaFile.parent.findFiles("**.{" + RES_EXTENSIONS.join(",") +"}")) {

                String path = metaParentPath
                    .relativize(file.getPath())
                    .toString()
                    .replace("\\", "/")

                module.resources.put(path, file.readAsBytes());
            }

            return module

        } catch (ParserConfigurationException | IOException | SAXException e) {
            log.error("Theme reading error. File: " + metaFile.getPath(), e)
            throw new RuntimeException(e)
        }
    }

    @Override
    void write(@NotNull EcosFile root, Module module, Unit config) {

        EcosFile dir = root.createDir(module.id)
        if (module.getResources() != null) {
            for (String key : module.getResources().keySet()) {
                dir.createFile(key, module.getResources().get(key));
            }
        }
        dir.createFile("meta.json", Json.getMapper().toBytes(module.meta));
    }

    static class ModuleMeta {
        MLText name
        Map<String, String> images = new HashMap<>()
    }

    static class Module {
        String id;
        ModuleMeta meta
        Map<String, byte[]> resources
    }
}
