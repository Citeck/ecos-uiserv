package eapps.types.ui.theme

import kotlin.Unit
import org.jetbrains.annotations.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.xml.sax.SAXException
import ru.citeck.ecos.apps.artifact.ArtifactMeta
import ru.citeck.ecos.apps.artifact.controller.ArtifactController
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.io.file.EcosFile
import ru.citeck.ecos.commons.json.Json

import javax.xml.parsers.ParserConfigurationException
import java.nio.file.Path
import java.util.stream.Collectors

class ThemeArtifactMeta {
    MLText name
    Map<String, String> images = new HashMap<>()
}

class Artifact {
    String id;
    ThemeArtifactMeta meta
    Map<String, byte[]> resources
}

return new ArtifactController<Artifact, Unit>() {

    // ThemeService should has the same constants
    public static final List<String> META_EXTENSIONS = Arrays.asList("yml", "yaml", "json");
    public static final List<String> RES_EXTENSIONS = Arrays.asList("png", "jpeg", "jpg", "svg", "ico", "css");

    private static final Logger log = LoggerFactory.getLogger(ArtifactController.class)

    @Override
    List<Artifact> read(@NotNull EcosFile root, Unit config) {

        return root.findFiles("**/meta.{" + META_EXTENSIONS.join(",") + "}")
            .stream()
            .map(this.&readModule)
            .collect(Collectors.toList())
    }

    private Artifact readModule(EcosFile metaFile) {

        try {

            Artifact module = new Artifact()
            module.id = metaFile.parent.name
            module.meta = Json.getMapper().read(metaFile, ThemeArtifactMeta.class)

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
    void write(@NotNull EcosFile root, Artifact module, Unit config) {

        EcosFile dir = root.createDir(module.id)
        if (module.getResources() != null) {
            for (String key : module.getResources().keySet()) {
                dir.createFile(key, module.getResources().get(key));
            }
        }
        dir.createFile("meta.json", Json.getMapper().toBytes(module.meta));
    }

    @Override
    ArtifactMeta getMeta(@NotNull Artifact artifact, @NotNull Unit unit) {
        return ArtifactMeta.create()
            .withId(artifact.id)
            .withName(artifact.meta.name)
            .build();
    }
}
