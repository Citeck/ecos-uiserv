package emtypes.ui.icon

import kotlin.Unit
import kotlin.jvm.functions.Function1
import org.jetbrains.annotations.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.citeck.ecos.apps.module.controller.ModuleController
import ru.citeck.ecos.commons.io.file.EcosFile
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.FileUtils
import ru.citeck.ecos.uiserv.service.icon.IconModule
import ru.citeck.ecos.uiserv.service.icon.dto.IconType

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.function.Function
import java.util.stream.Collectors

return new ModuleController<IconModule, Unit>() {
    private static final Logger log = LoggerFactory.getLogger(this.class)

    @Override
    List<IconModule> read(@NotNull EcosFile root, Unit config) {
        List<IconModule> collect = root.findFiles("**.{png,json}")
            .stream()
            .map(new Function<EcosFile, IconModule>() {
                @Override
                IconModule apply(EcosFile module) {
                    return readModule(root, module)
                }
            })
            .collect(Collectors.toList())

        log.error("before return '{}'", collect)
        return collect
    }

    private IconModule readModule(EcosFile root, EcosFile file) {
        Path path = file.getPath()

        if (path.toString().endsWith(".png")) {
            return pngModule(root, file)
        }

        if (path.toString().endsWith(".json")) {
            return jsonModule(file)
        }

        throw new RuntimeException("File has unsupported extension. Path: '" + file.getPath() + "'")
    }

    private IconModule pngModule(EcosFile root, EcosFile file) {
        byte[] fileContent = file.readAsBytes()

        String encodedString = Base64.getEncoder().encodeToString(fileContent)

        IconModule module = new IconModule()

        String id = root.getPath()
            .relativize(file.getPath())
            .toString()
            .replace("\\", "/")
        module.setId(id)
        module.setType(IconType.IMG)
        module.setFormat("png")
        module.setData(encodedString)

        return module
    }

    private IconModule jsonModule(EcosFile file) {
        String data = file.readAsString()

        return Json.getMapper().read(data, IconModule.class)
    }


    @Override
    void write(@NotNull EcosFile root, IconModule module, Unit config) {

        String extension
        byte[] data
        switch (module.getType()) {
            case IconType.FA:
                extension = ".json"
                data = Json.getMapper().toString(module).getBytes(StandardCharsets.UTF_8)
                break
            case IconType.IMG:
                extension = ".png"
                data = Base64.getDecoder().decode(module.getData())
                break
            default:
                throw new RuntimeException("File has unsupported type: " + module.getType())
        }

        String name = FileUtils.getValidName(module.getId()) + extension

        root.createFile(name, (Function1<OutputStream, Unit>) {
            OutputStream out -> out.write(data)
        })
    }
}
