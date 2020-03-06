package ru.citeck.ecos.uiserv.service.menu;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import ru.citeck.ecos.apps.EcosAppsApiFactory;
import ru.citeck.ecos.apps.app.module.type.ui.menu.MenuModule;
import ru.citeck.ecos.apps.utils.EappZipUtils;
import ru.citeck.ecos.apps.utils.io.mem.EappMemDir;
import ru.citeck.ecos.apps.utils.io.mem.EappMemFile;
import ru.citeck.ecos.uiserv.domain.FileType;
import ru.citeck.ecos.uiserv.service.file.FileService;
import ru.citeck.ecos.uiserv.web.rest.v1.dto.ModuleToDeploy;
import ru.citeck.ecos.uiserv.web.rest.v1.UpdaterApi;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class MenuModuleDeployConfig {

    private EappMemDir zipToDeploy = new EappMemDir("menu.zip");
    private EappMemFile menuXmlFile = (EappMemFile) zipToDeploy.createFile("menu.xml", new byte[0]);

    private EcosAppsApiFactory apiFactory;
    private UpdaterApi updaterController;
    private FileService fileService;

    private boolean initialized = false;

    public MenuModuleDeployConfig(EcosAppsApiFactory apiFactory,
                                  UpdaterApi updaterController,
                                  FileService fileService) {

        this.fileService = fileService;
        this.apiFactory = apiFactory;
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

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {

        if (initialized) {
            return;
        }
        apiFactory.getModuleApi().onModulePublished(MenuModule.class, this::deployMenu);
        apiFactory.getModuleApi().onModuleDeleted(MenuModule.class, this::deleteMenu);
        initialized = true;
    }

    public void deleteMenu(String menuId) {

        log.info("Menu delete msg: " + menuId);

        fileService.deployFileOverride(FileType.MENU, menuId, null, null, null);
    }

    public void deployMenu(MenuModule menuModule) {

        log.info("Menu module received: " + menuModule.getId());

        final ModuleToDeploy moduleToDeploy = new ModuleToDeploy();
        moduleToDeploy.mimeType = "application/zip";
        moduleToDeploy.key = "key";
        moduleToDeploy.version = 1;
        moduleToDeploy.type = "MENU";

        menuXmlFile.write(menuModule.getXmlData());
        moduleToDeploy.data = EappZipUtils.writeZipAsBytes(zipToDeploy);
        updaterController.deploy(moduleToDeploy);
    }
}
