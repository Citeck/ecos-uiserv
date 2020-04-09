package ru.citeck.ecos.uiserv.service.menu;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.uiserv.domain.MenuConfigurationDto;
import ru.citeck.ecos.uiserv.domain.MenuConfigurationEntity;
import ru.citeck.ecos.uiserv.repository.MenuConfigurationRepository;
import ru.citeck.ecos.uiserv.web.rest.menu.xml.MenuConfig;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Service
@Transactional
public class MenuConfigurationService {
    private final MenuConfigurationRepository repository;
    private final JAXBContext jaxbContext;

    public MenuConfigurationService(MenuConfigurationRepository repository) {
        this.repository = repository;
        try {
            jaxbContext = JAXBContext.newInstance(MenuConfig.class);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<MenuConfigurationDto> getMenu(String menuId) {
        return repository.findByExtId(menuId).map(this::mapToDto);
    }

    private MenuConfigurationDto mapToDto(MenuConfigurationEntity entity) {
        if (entity == null) {
            return null;
        }

        MenuConfigurationDto dto = new MenuConfigurationDto();

        dto.setId(entity.getExtId());
        dto.setType(entity.getType());
        dto.setAuthorities(entity.getAuthorities());
        dto.setConfig(unmarshal(entity.getConfig().getBytes()));
        dto.setLocalization(Json.getMapper().convert(entity.getLocalization(), MenuConfigurationDto.LocalizationMap.class));
        dto.setModelVersion(entity.getModelVersion());

        return dto;
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
}
