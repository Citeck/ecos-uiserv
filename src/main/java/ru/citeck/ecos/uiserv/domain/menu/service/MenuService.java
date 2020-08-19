package ru.citeck.ecos.uiserv.domain.menu.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.uiserv.app.application.props.ApplicationProperties;
import ru.citeck.ecos.uiserv.domain.menu.repo.MenuEntity;
import ru.citeck.ecos.uiserv.domain.menu.repo.MenuRepository;
import ru.citeck.ecos.uiserv.app.common.service.AuthoritiesSupport;
import ru.citeck.ecos.uiserv.domain.i18n.service.I18nService;
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDeployModule;
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDto;
import ru.citeck.ecos.uiserv.domain.menu.dto.SubMenuDto;
import ru.citeck.ecos.uiserv.domain.menu.service.format.MenuReaderService;
import ru.citeck.ecos.uiserv.domain.menu.service.resolving.MenuFactory;
import ru.citeck.ecos.uiserv.domain.menu.service.resolving.ResolvedMenuDto;
import ru.citeck.ecos.uiserv.domain.menu.service.resolving.resolvers.MenuItemsResolver;

import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
@RequiredArgsConstructor
public class MenuService {

    private static final String DEFAULT_AUTHORITY = "default";
    private static final String DEFAULT_MENU_ID = "default-menu";
    private static final String DEFAULT_MENU_V1_ID = "default-menu-v1";

    private final MenuRepository menuRepository;
    private final MenuReaderService readerService;
    private final I18nService i18nService;
    private final AuthoritiesSupport authoritiesSupport;
    private final ApplicationProperties applicationProperties;
    private final List<MenuItemsResolver> resolvers;

    public long getLastModifiedTimeMs() {
        return menuRepository.getLastModifiedTime()
            .map(Instant::toEpochMilli)
            .orElse(0L);
    }

    public MenuDto upload(MenuDeployModule module) {
        MenuDto menuDto = readerService.readMenu(module.getData(), module.getFilename());
        MenuEntity entity = mapToEntity(menuDto);
        return mapToDto(menuRepository.save(entity));
    }

    public Set<String> getAllAuthoritiesWithMenu() {
        return menuRepository.getAllAuthoritiesWithMenu();
    }

    public List<MenuDto> getAllMenus() {
        return menuRepository.findAll()
            .stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    public Optional<MenuDto> getMenu(String menuId) {
        if (StringUtils.isBlank(menuId)) {
            return Optional.empty();
        }
        return menuRepository.findByExtId(menuId).map(this::mapToDto);
    }

    private MenuEntity mapToEntity(MenuDto menuDto) {

        MenuEntity entity = menuRepository.findByExtId(menuDto.getId()).orElse(null);
        if (entity == null) {
            entity = new MenuEntity();
            entity.setExtId(menuDto.getId());
        }

        entity.setTenant("");
        entity.setType(menuDto.getType());
        entity.setAuthorities(new ArrayList<>(menuDto.getAuthorities()));
        entity.setVersion(menuDto.getVersion());
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
        dto.setAuthorities(new ArrayList<>(entity.getAuthorities()));
        dto.setSubMenu(Json.getMapper().read(entity.getItems(), SubMenus.class));
        dto.setVersion(entity.getVersion());

        return dto;
    }

    public MenuDto getMenuForUser(String username, Integer version) {
        Set<String> userAuthorities = new HashSet<>(authoritiesSupport.queryUserAuthorities(username));
        return getMenuForUser(username, userAuthorities, version);
    }

    public MenuDto getMenuForUser(String username, Set<String> authorities, Integer version) {

        return Stream.<Supplier<Optional<MenuDto>>>of(
            () -> findMenuByUsername(username),
            () -> findByAuthorities(authorities),
            this::findByDefaultAuthority
        ).map(Supplier::get)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(config -> {
                int confVersion = config.getVersion() != null ? config.getVersion() : 0;
                int argVersion = version != null ? version : 0;
                return confVersion == argVersion;
            })
            .findFirst()
            .orElseGet(() -> findDefaultMenu(version));
    }

    public ResolvedMenuDto getResolvedMenuForUser(String username) {
        Set<String> userAuthorities = new HashSet<>(authoritiesSupport.queryUserAuthorities(username));
        MenuDto dto = getMenuForUser(username, userAuthorities, null);
        return resolveMenu(dto, userAuthorities);
    }

    private Optional<MenuDto> findMenuByUsername(String username) {
        return getMenu(username);
    }

    private Optional<MenuDto> findByAuthorities(Set<String> userAuthorities) {
        List<String> orderedAuthorities = getOrderedAuthorities(userAuthorities);

        return findFirstByAuthorities(orderedAuthorities);
    }

    private Optional<MenuDto> findByDefaultAuthority() {
        return findFirstByAuthorities(Collections.singletonList(DEFAULT_AUTHORITY));
    }

    private Optional<MenuDto> findFirstByAuthorities(List<String> authorities) {
        return authorities.stream()
            .map(menuRepository::findAllByAuthoritiesContains)
            .flatMap(List::stream)
            .filter(entity -> !DEFAULT_MENU_ID.equals(entity.getExtId()))
            .map(this::mapToDto)
            .findFirst();
    }

    private List<String> getOrderedAuthorities(Set<String> userAuthorities) {
        Set<String> allUserAuthorities = new HashSet<>(userAuthorities);
        List<String> defaultOrder = new ArrayList<>(applicationProperties.getMenuConfigAuthorityOrder());
        defaultOrder.retainAll(allUserAuthorities);
        allUserAuthorities.removeAll(defaultOrder);

        List<String> orderedAuthorities = new LinkedList<>();
        orderedAuthorities.addAll(defaultOrder);
        orderedAuthorities.addAll(allUserAuthorities);
        return orderedAuthorities;
    }

    private MenuDto findDefaultMenu(Integer version) {
        int intVersion = version != null ? version : 0;
        Optional<MenuDto> result;
        if (intVersion == 1) {
            result = getMenu(DEFAULT_MENU_V1_ID);
        } else {
            result = getMenu(DEFAULT_MENU_ID);
        }
        return result.orElseThrow(() -> new RuntimeException("Cannot load default menu. Version: " + version));
    }

    private ResolvedMenuDto resolveMenu(MenuDto menuDto, Set<String> allUserAuthorities) {
        if (menuDto == null) {
            return null;
        }
        return new MenuFactory(
            allUserAuthorities,
            i18nService::getMessage,
            resolvers
        ).getResolvedMenu(menuDto);
    }

    public MenuDto save(MenuDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Dto cannot be null");
        }

        if (StringUtils.isBlank(dto.getId())) {
            throw new IllegalArgumentException("Id param in menu dto is mandatory");
        }

        return mapToDto(menuRepository.save(mapToEntity(dto)));
    }

    public void deleteByExtId(String menuId) {
        if (StringUtils.isBlank(menuId)
            || menuId.equals(DEFAULT_MENU_ID)
            || menuId.equals(DEFAULT_MENU_V1_ID)) {

            return;
        }
        menuRepository.deleteByExtId(menuId);
    }

    public static class SubMenus extends HashMap<String, SubMenuDto> {
    }
}
