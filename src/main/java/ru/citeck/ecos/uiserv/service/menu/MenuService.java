package ru.citeck.ecos.uiserv.service.menu;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.uiserv.domain.MenuEntity;
import ru.citeck.ecos.uiserv.repository.MenuRepository;
import ru.citeck.ecos.uiserv.service.menu.dto.MenuDto;
import ru.citeck.ecos.uiserv.service.menu.dto.MenuItemDto;
import ru.citeck.ecos.uiserv.service.menu.format.MenuReaderService;

import java.util.ArrayList;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository repository;
    private final MenuReaderService readerService;

    public MenuDto upload(MenuDeployModule module) {
        MenuDto menuDto = readerService.readMenu(module.getData(), module.getFilename());
        MenuEntity entity = mapToEntity(menuDto);
        return mapToDto(repository.save(entity));
    }

    public Optional<MenuDto> getMenu(String menuId) {
        return repository.findByExtId(menuId).map(this::mapToDto);
    }

    private MenuEntity mapToEntity(MenuDto menuDto) {

        MenuEntity entity = repository.findByExtId(menuDto.getId()).orElse(null);
        if (entity == null) {
            entity = new MenuEntity();
            entity.setExtId(menuDto.getId());
        }

        entity.setTenant("");
        entity.setType(menuDto.getType());
        entity.setAuthorities(Json.getMapper().toString(menuDto.getAuthorities()));
        entity.setPriority(menuDto.getPriority());
        entity.setItems(Json.getMapper().toString(menuDto.getItems()));

        return entity;
    }

    private MenuDto mapToDto(MenuEntity entity) {

        if (entity == null) {
            return null;
        }

        MenuDto dto = new MenuDto();

        dto.setId(entity.getExtId());
        dto.setType(entity.getType());
        dto.setAuthorities(Json.getMapper().read(entity.getAuthorities(), StrList.class));
        dto.setItems(Json.getMapper().read(entity.getItems(), MenuItems.class));
        dto.setPriority(entity.getPriority());

        return dto;
    }

    public static class StrList extends ArrayList<String> {}
    public static class MenuItems extends ArrayList<MenuItemDto> {}
}
