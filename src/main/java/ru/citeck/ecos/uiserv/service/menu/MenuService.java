package ru.citeck.ecos.uiserv.service.menu;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
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
        dto.setAuthorities(new ArrayList<>(entity.getAuthorities()));
        dto.setSubMenu(Json.getMapper().read(entity.getItems(), SubMenus.class));
        dto.setPriority(entity.getPriority());

        return dto;
    }

    public ResolvedMenuDto getMenuForUser(String username) {
        final Set<String> userAuthorities = new HashSet<>(authoritiesSupport.queryUserAuthorities(username));

        MenuDto foundDto = Stream.<Supplier<Optional<MenuDto>>>of(
            () -> findMenuByUsername(username),
            () -> findByAuthorities(userAuthorities),
            this::findByDefaultAuthority
        ).map(Supplier::get)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst()
            .orElseGet(this::findDefaultMenu);

        return resolveMenu(foundDto, userAuthorities);
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

    private MenuDto findDefaultMenu() {
        return getMenu(DEFAULT_MENU_ID)
            .orElseThrow(() -> new RuntimeException("Cannot load default menu"));
    }

    private ResolvedMenuDto resolveMenu(MenuDto menuDto, Set<String> allUserAuthorities) {
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
        if (StringUtils.isBlank(menuId)) {
            return;
        }

        menuRepository.deleteByExtId(menuId);
    }

    public static class SubMenus extends HashMap<String, SubMenuDto> {
    }
}
