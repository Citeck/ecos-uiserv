package ru.citeck.ecos.uiserv.service.menu;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import ru.citeck.ecos.apps.module.handler.EcosModuleHandler;
import ru.citeck.ecos.apps.module.handler.ModuleMeta;
import ru.citeck.ecos.apps.module.handler.ModuleWithMeta;
import ru.citeck.ecos.commons.io.file.mem.EcosMemDir;
import ru.citeck.ecos.commons.utils.ZipUtils;
import ru.citeck.ecos.uiserv.service.file.FileService;
import ru.citeck.ecos.uiserv.web.rest.v1.dto.ModuleToDeploy;
import ru.citeck.ecos.uiserv.web.rest.v1.UpdaterApi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.function.Consumer;

@Slf4j
@Configuration
public class MenuModuleDeployConfig implements EcosModuleHandler<MenuModule> {

    private EcosMemDir zipToDeploy = new EcosMemDir();

    private UpdaterApi updaterController;

    public MenuModuleDeployConfig(UpdaterApi updaterController,
                                 FileService fileService) {

        this.updaterController = updaterController;

        //Temp solution. Will be changed
        ClassPathResource ruMenuLocale = new ClassPathResource("/menu/default-menu_ru.properties");
        ClassPathResource enMenuLocale = new ClassPathResource("/menu/default-menu_en.properties");

        try (InputStream ru = ruMenuLocale.getInputStream();
             InputStream en = enMenuLocale.getInputStream())  {

            zipToDeploy.createFile("menu_ru.properties", IOUtils.toByteArray(ru));
            zipToDeploy.createFile("menu_en.properties", IOUtils.toByteArray(en));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deployModule(@NotNull MenuModule menuModule) {

        log.info("Menu module received: " + menuModule.getId());

        final ModuleToDeploy moduleToDeploy = new ModuleToDeploy();
        moduleToDeploy.mimeType = "application/zip";
        moduleToDeploy.key = "key";
        moduleToDeploy.version = 1;
        moduleToDeploy.type = "MENU";

        EcosMemDir deployDir = new EcosMemDir();
        deployDir.copyFilesFrom(zipToDeploy);
        deployDir.createFile("menu.xml", menuModule.getXmlData());

        moduleToDeploy.data = ZipUtils.writeZipAsBytes(deployDir);
        updaterController.deploy(moduleToDeploy);
    }

    @NotNull
    @Override
    public ModuleWithMeta<MenuModule> getModuleMeta(@NotNull MenuModule menuModule) {
        return new ModuleWithMeta<>(menuModule, new ModuleMeta(menuModule.getId(), Collections.emptyList()));
    }

    @Override
    public void listenChanges(@NotNull Consumer<MenuModule> consumer) {
    }

    @Nullable
    @Override
    public ModuleWithMeta<MenuModule> prepareToDeploy(@NotNull MenuModule menuModule) {
        return getModuleMeta(menuModule);
    }

    @NotNull
    @Override
    public String getModuleType() {
        return "ui/menu";
    }
}
