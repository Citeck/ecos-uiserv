package ru.citeck.ecos.uiserv.service.menu;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import ru.citeck.ecos.apps.module.listener.EcosModuleListener;
import ru.citeck.ecos.commons.io.file.mem.EcosMemDir;
import ru.citeck.ecos.commons.utils.ZipUtils;
import ru.citeck.ecos.uiserv.domain.FileType;
import ru.citeck.ecos.uiserv.service.file.FileService;
import ru.citeck.ecos.uiserv.web.rest.v1.dto.ModuleToDeploy;
import ru.citeck.ecos.uiserv.web.rest.v1.UpdaterApi;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class MenuModuleDeployConfig implements EcosModuleListener<MenuModule> {

    private EcosMemDir zipToDeploy = new EcosMemDir();

    private UpdaterApi updaterController;
    private FileService fileService;

    public MenuModuleDeployConfig(UpdaterApi updaterController,
                                 FileService fileService) {

        this.fileService = fileService;
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
    public void onModuleDeleted(@NotNull String menuId) {
        log.info("Menu delete msg: " + menuId);
        fileService.deployFileOverride(FileType.MENU, menuId, null, null, null);
    }

    @Override
    public void onModulePublished(MenuModule menuModule) {

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
    public String getModuleType() {
        return "ui/menu";
    }
}
