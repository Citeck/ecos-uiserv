package ru.citeck.ecos.uiserv.service.menu;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.uiserv.domain.File;
import ru.citeck.ecos.uiserv.domain.FileType;
import ru.citeck.ecos.uiserv.service.file.FileService;
import ru.citeck.ecos.uiserv.service.file.FileViewCaching;
import ru.citeck.ecos.uiserv.web.rest.menu.xml.MenuConfig;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * @deprecated for removal
 * use {@link MenuConfigurationService}
 */
@Service
@Transactional
@Deprecated
public class MenuService {
    private final JAXBContext jaxbContext;
    private final FileViewCaching<MenuView> caching;

    public MenuService(FileService fileService) {
        try {
            jaxbContext = JAXBContext.newInstance(MenuConfig.class);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
        this.caching = new FileViewCaching<>(
            key -> fileService.loadFile(FileType.MENU, key),
            this::menuViewOf);
    }

    public Optional<MenuView> getMenu(String menuId) {
        return caching.get(menuId);
    }

    private Optional<MenuView> menuViewOf(File x) {
        // No XML means "hidden" menu, so we unsee it despite we just loaded it :)
        if (x.getFileVersion().getBytes() == null)
            return Optional.empty();
        return Optional.of(new MenuView(
            x.getFileVersion().getTranslated().getId(),
            unmarshal(x.getFileVersion().getBytes()),
            x.getFileVersion().getProductVersion()));
    }

    private MenuConfig unmarshal(byte[] xml) {
        try {
            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            try (final InputStream input = new ByteArrayInputStream(xml)) {
                return (MenuConfig) unmarshaller.unmarshal(input);
            }
        } catch (JAXBException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] marshal(MenuConfig xml) {
        try {
            final Marshaller marshaller = jaxbContext.createMarshaller();
            try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                marshaller.marshal(xml, output);
                return output.toByteArray();
            }
        } catch (JAXBException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class MenuView {
        public final Long translatedEntityId;
        public final MenuConfig xml;
        public final Long productVersion;

        public MenuView(Long translatedEntityId, MenuConfig xml,
                        Long productVersion) {
            this.translatedEntityId = translatedEntityId;
            this.xml = xml;
            this.productVersion = productVersion;
        }
    }

    @Bean
    public FileService.FileMetadataExtractorInfo menuFileMetadataExtractor() {
        return new FileService.FileMetadataExtractorInfo(FileType.MENU,
            bytes -> unmarshal(bytes).getId());
    }
}
