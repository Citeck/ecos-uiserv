package ru.citeck.ecos.uiserv.service.menu;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.uiserv.config.ApplicationProperties;
import ru.citeck.ecos.uiserv.domain.MenuEntity;
import ru.citeck.ecos.uiserv.repository.MenuRepository;
import ru.citeck.ecos.uiserv.service.AuthoritiesSupport;
import ru.citeck.ecos.uiserv.service.i18n.I18nService;
import ru.citeck.ecos.uiserv.service.menu.dto.MenuDto;
import ru.citeck.ecos.uiserv.service.menu.dto.SubMenuDto;
import ru.citeck.ecos.uiserv.service.menu.format.MenuReaderService;
import ru.citeck.ecos.uiserv.service.menu.resolving.MenuFactory;
import ru.citeck.ecos.uiserv.service.menu.resolving.ResolvedMenuDto;
import ru.citeck.ecos.uiserv.service.menu.resolving.resolvers.MenuItemsResolver;

import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class MenuService {
    private static final String DEFAULT_AUTHORITY = "default";
    private static final String DEFAULT_MENU_ID = "default-menu";

    private final MenuRepository repository;
    private final MenuReaderService readerService;
    private final I18nService i18nService;
    private final AuthoritiesSupport authoritiesSupport;
    private final ApplicationProperties applicationProperties;
    private final List<MenuItemsResolver> resolvers;

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
        entity.setItems(Json.getMapper().toString(menuDto.getSubMenu()));

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
        dto.setSubMenu(Json.getMapper().read(entity.getItems(), SubMenus.class));
        dto.setPriority(entity.getPriority());

        return dto;
    }

    public ResolvedMenuDto getMenuForUser(String username) {
        final Set<String> allUserAuthorities = new HashSet<>(authoritiesSupport.queryUserAuthorities(username));

        return getOrderedAuthorities(allUserAuthorities, username)
            .stream()
            .map(this::getMenu)
            .filter(Optional::isPresent)
            .findFirst()
            .orElse(this.getMenu(DEFAULT_AUTHORITY))
            .map(menuDto -> loadMenuFromStore(menuDto, allUserAuthorities))
            .orElseGet(() -> this.loadDefaultMenu(allUserAuthorities));
    }

    private List<String> getOrderedAuthorities(Set<String> userAuthorities, String userName) {
        Set<String> allUserAuthorities = new HashSet<>(userAuthorities);
        String defaultOrderParam = applicationProperties.getMenuConfigAuthorityOrder();
        List<String> defaultOrder = new ArrayList<>(Arrays.asList(defaultOrderParam.split(",")));
        defaultOrder.retainAll(allUserAuthorities);
        allUserAuthorities.removeAll(defaultOrder);
        allUserAuthorities.remove(userName);

        List<String> orderedAuthorities = new LinkedList<>();
        orderedAuthorities.add(userName);
        orderedAuthorities.addAll(defaultOrder);
        orderedAuthorities.addAll(allUserAuthorities);
        return orderedAuthorities;
    }

    private ResolvedMenuDto loadDefaultMenu(Set<String> authorities) {
        return this.getMenu(DEFAULT_MENU_ID)
            .map(menuDto -> loadMenuFromStore(menuDto, authorities))
            .orElseThrow(() -> new RuntimeException("Cannot load default menu"));
    }

    private ResolvedMenuDto loadMenuFromStore(MenuDto menuDto, Set<String> allUserAuthorities) {
        return new MenuFactory(
            allUserAuthorities,
            i18nService::getMessage,
            resolvers
        ).getResolvedMenu(menuDto);
    }

    public static class StrList extends ArrayList<String> {
    }

    public static class SubMenus extends HashMap<String, SubMenuDto> {
    }
}
