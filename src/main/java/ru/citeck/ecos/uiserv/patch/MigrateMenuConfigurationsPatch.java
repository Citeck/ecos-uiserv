package ru.citeck.ecos.uiserv.patch;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.metarepo.EcosMetaRepo;
import ru.citeck.ecos.uiserv.domain.*;
import ru.citeck.ecos.uiserv.repository.MenuConfigurationRepository;
import ru.citeck.ecos.uiserv.repository.TranslationRepository;
import ru.citeck.ecos.uiserv.service.file.FileService;
import ru.citeck.ecos.uiserv.service.translation.TranslationService;
import ru.citeck.ecos.uiserv.web.rest.menu.xml.MenuConfig;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@DependsOn("liquibase")
public class MigrateMenuConfigurationsPatch {

    private static final String PATCH_KEY = "patch.MigrateMenuConfigurationsPatch";

    private static final Integer BATCH_SIZE = 10000;

    private final EcosMetaRepo metaRepo;
    private final FileService fileService;
    private final TranslationService i18n;
    private final JAXBContext jaxbContext;
    private final TranslationRepository translationRepository;
    private final MenuConfigurationRepository menuConfigurationRepository;

    public MigrateMenuConfigurationsPatch(EcosMetaRepo metaRepo,
                                          FileService fileService,
                                          TranslationService i18n,
                                          TranslationRepository translationRepository,
                                          MenuConfigurationRepository menuConfigurationRepository) {
        try {
            jaxbContext = JAXBContext.newInstance(MenuConfig.class);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
        this.metaRepo = metaRepo;
        this.fileService = fileService;
        this.i18n = i18n;
        this.translationRepository = translationRepository;
        this.menuConfigurationRepository = menuConfigurationRepository;
    }

    @PostConstruct
    @Transactional
    public void execute() {

        PatchStatus currentStatus = metaRepo.get(PATCH_KEY, PatchStatus.class);
        if (currentStatus != null) {
            return;
        }

        PatchStatus status = new PatchStatus();

        int processed = 0;

        List<File> menus;
        while (!(menus = fileService.findByType(FileType.MENU, BATCH_SIZE, processed)).isEmpty()) {
            for (File menu : menus) {
                MenuConfigurationEntity entity = fileToEntity(menu);
                if (entity == null) {
                    status.skipped++;
                    status.skippedMenus.add(menu.getFileId());
                    continue;
                }

                menuConfigurationRepository.save(entity);

                status.total++;
                status.migrated.add(menu.getFileId());
            }

            processed += BATCH_SIZE;
        }

        metaRepo.put(PATCH_KEY, status);
    }

    private MenuConfigurationEntity fileToEntity(File file) {
        MenuConfig config;
        try {
            config = unmarshal(file.getFileVersion().getBytes());
        } catch (IOException | JAXBException e) {
            log.error("Failed to unmarshal menu config with id='{}'", file.getFileVersion().getId());
            return null;
        }

        MenuConfigurationEntity entity = new MenuConfigurationEntity();
        entity.setExtId(file.getFileId());
        entity.setType(config.getType());
        entity.setAuthorities(config.getAuthorities());
        entity.setConfig(Json.getMapper().toString(config));
        entity.setModelVersion(0);
        Long translatedId = file.getFileVersion().getTranslated().getId();
        entity.setLocalization(Json.getMapper().toString(loadLocalization(translatedId)));
        return entity;
    }

    private MenuConfig unmarshal(byte[] xml) throws IOException, JAXBException {
        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        try (final InputStream input = new ByteArrayInputStream(xml)) {
            return (MenuConfig) unmarshaller.unmarshal(input);
        }
    }

    private MenuConfigurationDto.LocalizationMap loadLocalization(Long translatedId) {
        List<Translation> translations = translationRepository.findAllByTranslatedId(translatedId);

        Map<Locale, Map<String, String>> localizationMap = translations.stream()
            .map(this::getLocalizationMapFromTranslation)
            .flatMap(m -> m.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return new MenuConfigurationDto.LocalizationMap(localizationMap);
    }

    private Map<Locale, Map<String, String>> getLocalizationMapFromTranslation(Translation translation) {
        ResourceBundle bundle = i18n.toBundle(translation.getBundle());
        Enumeration<String> keysEnumerator = bundle.getKeys();

        Map<String, String> stringsMap = new HashMap<>();
        while (keysEnumerator.hasMoreElements()) {
            String key = keysEnumerator.nextElement();
            String value = bundle.getString(key);

            stringsMap.put(key, value);
        }

        Map<Locale, Map<String, String>> localizationMap = new HashMap<>();
        localizationMap.put(bundle.getLocale(), stringsMap);
        return localizationMap;
    }

    @Data
    public static class PatchStatus {

        private List<String> migrated = new ArrayList<>();
        private List<String> skippedMenus = new ArrayList<>();
        private int skipped = 0;
        private int total = 0;
    }
}
