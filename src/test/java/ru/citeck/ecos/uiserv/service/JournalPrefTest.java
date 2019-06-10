package ru.citeck.ecos.uiserv.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.FileType;
import ru.citeck.ecos.uiserv.service.file.FileService;
import ru.citeck.ecos.uiserv.service.journal.JournalPrefService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class JournalPrefTest {
    @Autowired
    private FileService fileService;

    @Autowired
    private JournalPrefService journalPrefService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    public void testDeployPref() throws IOException {
        try (ByteArrayOutputStream bs = new ByteArrayOutputStream()) {
            objectMapper.writeValue(bs, this);
            fileService.deployStandardFile(FileType.JOURNALPREFS, "contract-agreements-default",
                null, bs.toByteArray(), 1L);
        }

        //need transaction here because of JPA lazyness
//        new TransactionTemplate(transactionManager).execute(x -> {
//            final Optional<MenuService.MenuView> check = journalPrefService.getMenu(menuId);
//            //some tests on "check" and "versionRepository", maybe manual via debugging
//            return check;
//        });
    }
}
