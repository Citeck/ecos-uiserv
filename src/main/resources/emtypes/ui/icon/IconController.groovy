package emtypes.ui.icon

import kotlin.Unit
import kotlin.jvm.functions.Function1
import org.jetbrains.annotations.NotNull
import ru.citeck.ecos.apps.module.controller.ModuleController
import ru.citeck.ecos.commons.io.file.EcosFile
import ru.citeck.ecos.commons.utils.FileUtils

import java.util.function.Function
import java.util.stream.Collectors

return new ModuleController<Module, Unit>() {
    @Override
    List<Module> read(@NotNull EcosFile root, Unit config) {
        List<Module> collect = root.findFiles("**.{png,json}")
            .stream()
            .map(new Function<EcosFile, Module>() {
                @Override
                Module apply(EcosFile module) {
                    return readModule(root, module)
                }
            })
            .collect(Collectors.toList())

        return collect
    }

    private Module readModule(EcosFile root, EcosFile file) {
        String filename = root.getPath()
            .relativize(file.getPath())
            .toString()
            .replace("\\", "/")

        byte[] data = file.readAsBytes()

        return new Module(filename: filename, data: data)
    }

    @Override
    void write(@NotNull EcosFile root, Module module, Unit config) {

        String name = FileUtils.getValidName(module.filename)

        root.createFile(name, (Function1<OutputStream, Unit>) {
            OutputStream out -> out.write(module.data)
        })
    }

    static class Module {
        String filename
        byte[] data
    }
}
