package ru.citeck.ecos.uiserv.web.rest;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.repository.FileVersionRepository;
import ru.citeck.ecos.uiserv.service.journal.JournalConfigService;
import ru.citeck.ecos.uiserv.service.menu.MenuService;
import ru.citeck.ecos.uiserv.service.menu.dto.MenuDto;
import ru.citeck.ecos.uiserv.web.rest.v1.dto.ModuleToDeploy;
import ru.citeck.ecos.uiserv.web.rest.v1.UpdaterApi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class UpdaterControllerTest {
    @Autowired
    private UpdaterApi updaterController;

    @Autowired
    private MenuService menuService;

    @Autowired
    private JournalConfigService journalConfigService;

    @Autowired
    private FileVersionRepository versionRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    //I need this test mostly because it's really awkward
    // to use curl for sending JSONs with byte[]
    @Test
    public void testDeployMenu() throws IOException {
        final ModuleToDeploy moduleToDeploy = new ModuleToDeploy();
        moduleToDeploy.mimeType = "applICAtion/zip";
        moduleToDeploy.key = "key";
        moduleToDeploy.version = 1;
        moduleToDeploy.type = "MENU";
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             InputStream input = new ClassPathResource("/default-menu.zip").getInputStream()) {
            IOUtils.copy(input, output);
            moduleToDeploy.data = output.toByteArray();
        }
        final String menuId = updaterController.deploy(moduleToDeploy);

        //need transaction here because of JPA lazyness
        new TransactionTemplate(transactionManager).execute(x -> {
            final Optional<MenuDto> check = menuService.getMenu(menuId);
            //some tests on "check" and "versionRepository", maybe manual via debugging
            return check;
        });
    }

    //Same thing
    //TODO: fix test
    /*@Test
    public void testDeployJournalConfig() throws IOException {
        final ModuleToDeploy moduleToDeploy = new ModuleToDeploy();
        moduleToDeploy.mimeType = "applICAtion/zip";
        moduleToDeploy.key = "key";
        moduleToDeploy.version = 1;
        moduleToDeploy.type = "JOURNALCFG";
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             InputStream input = new ClassPathResource("/currency.json.zip").getInputStream()) {
            IOUtils.copy(input, output);
            moduleToDeploy.data = output.toByteArray();
        }
        final String cfgId = updaterController.deploy(moduleToDeploy);

        //todo mock alfresco rest client
        //need transaction here because of JPA lazyness
        new TransactionTemplate(transactionManager).execute(x -> {
            final Optional<JournalConfigService.JournalConfigDownstream> check = journalConfigService.getJournalConfig(cfgId);
            //some tests on "check" and "versionRepository", maybe manual via debugging
            return check;
        });
    }*/
}
