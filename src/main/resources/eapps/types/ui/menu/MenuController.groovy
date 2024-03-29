package eapps.types.ui.menu

import kotlin.Unit
import kotlin.jvm.functions.Function1
import org.jetbrains.annotations.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.xml.sax.SAXException
import ru.citeck.ecos.apps.artifact.ArtifactMeta
import ru.citeck.ecos.apps.artifact.controller.ArtifactController
import ru.citeck.ecos.apps.artifact.controller.patch.ArtifactPatch
import ru.citeck.ecos.apps.artifact.controller.patch.ArtifactPatchController
import ru.citeck.ecos.apps.artifact.controller.type.JsonArtifactController
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.io.file.EcosFile
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.FileUtils

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import java.util.stream.Collectors

interface MenuControllerInterface<T> extends ArtifactController<T, Unit>, ArtifactPatchController<T, Unit> {}

class Artifact {
    String id
    String filename
    byte[] data

    Artifact() {}

    Artifact(Artifact other) {
        this.id = other.id
        this.filename = other.filename
        this.data = other.data
    }
}

return new MenuControllerInterface<Artifact>() {

    private static final Logger log = LoggerFactory.getLogger(ArtifactController.class)
    private static final Set<String> JSON_EXTENSIONS = Arrays.asList(".json", ".yml", ".yaml")

    @Override
    Artifact applyPatch(Artifact artifact, Unit config, ArtifactPatch patch) {
        try {
            JsonArtifactController.class.getMethod("applyPatch", ObjectData.class, ArtifactPatch.class)
        } catch (Throwable ignored) {
            println("Method JsonArtifactController.applyPatch doesn't found")
            return artifact
        }
        boolean isJsonExt = false
        for (String extension in JSON_EXTENSIONS) {
            if (!artifact.filename.endsWith(extension)) {
                isJsonExt = true
                break
            }
        }
        if (!isJsonExt) {
            log.warn("Patches allowed only for json-like artifacts")
            return artifact
        }
        def jsonData = Json.mapper.readData(artifact.data)
        ObjectData patchResult = JsonArtifactController.applyPatch(jsonData.asObjectData(), patch)

        def newArtifact = new Artifact(artifact)
        newArtifact.data = Json.mapper.toBytesNotNull(patchResult)
        def fileName = artifact.filename
        if (!fileName.endsWith(".json")) {
            fileName = fileName.substring(0, fileName.lastIndexOf('.')) + ".json"
        }
        newArtifact.filename = fileName
        return newArtifact
    }

    @Override
    List<Artifact> read(@NotNull EcosFile root, Unit config) {

        return root.findFiles("**.{json,xml}")
            .stream()
            .map(this.&readModule)
            .collect(Collectors.toList())
    }

    private Artifact readModule(EcosFile file) {

        byte[] data = file.readAsBytes()

        try {
            Artifact module = new Artifact()
            module.setFilename(file.getPath().getFileName().toString())
            module.setId(getMenuId(data, module.getFilename()))
            module.setData(data)
            return module

        } catch (ParserConfigurationException | IOException | SAXException e) {
            log.error("Menu reading error. File: " + file.getPath(), e)
            throw new RuntimeException(e)
        }
    }

    private String getMenuId(byte[] data, String fileName) throws ParserConfigurationException,
                                                                  IOException, SAXException {

        if (fileName.endsWith(".json")) {

            return Json.getMapper().read(data, ObjectData.class).get("id").asText()

        } else {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance()
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder()
            Document document = docBuilder.parse(new ByteArrayInputStream(data))

            return document.getElementsByTagName("id").item(0).getTextContent()
        }
    }

    @Override
    void write(@NotNull EcosFile root, Artifact artifact, Unit config) {

        String extension = ".xml";
        if (artifact.getFilename().endsWith(".json")) {
            extension = ".json";
        }
        String name = FileUtils.getValidName(artifact.getId()) + extension;

        root.createFile(name, (Function1<OutputStream, Unit>) {
            OutputStream out -> out.write(artifact.getData())
        })
    }

    @Override
    ArtifactMeta getMeta(@NotNull Artifact artifact, @NotNull Unit unit) {
        return ArtifactMeta.create()
            .withId(artifact.id)
            .build();
    }

}
