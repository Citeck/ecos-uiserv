package ru.citeck.ecos.uiserv.domain.journal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.file.repo.FileType;
import ru.citeck.ecos.uiserv.domain.file.service.FileService;
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@ExtendWith(EcosSpringExtension.class)
@SpringBootTest(classes = Application.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
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
