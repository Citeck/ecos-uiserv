package ru.citeck.ecos.uiserv.domain.menu.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.uiserv.domain.config.api.records.ConfigRecords;
import ru.citeck.ecos.uiserv.domain.menu.repo.MenuEntity;
import ru.citeck.ecos.uiserv.domain.menu.repo.MenuRepository;
import ru.citeck.ecos.uiserv.app.common.service.AuthoritiesSupport;
import ru.citeck.ecos.uiserv.domain.i18n.service.I18nService;
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDeployArtifact;
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDto;
import ru.citeck.ecos.uiserv.domain.menu.dto.SubMenuDef;
import ru.citeck.ecos.uiserv.domain.menu.service.format.MenuReaderService;
import ru.citeck.ecos.uiserv.domain.menu.service.resolving.MenuFactory;
import ru.citeck.ecos.uiserv.domain.menu.service.resolving.ResolvedMenuDto;
import ru.citeck.ecos.uiserv.domain.menu.service.resolving.resolvers.MenuItemsResolver;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class MenuService {

    private static final String DEFAULT_AUTHORITY = "GROUP_EVERYONE";
    private static final String DEFAULT_MENU_ID = "default-menu";
    private static final String DEFAULT_MENU_V1_ID = "default-menu-v1";

    public static final List<String> DEFAULT_MENUS = Arrays.asList(
        DEFAULT_MENU_ID,
        DEFAULT_MENU_V1_ID
    );
    private final MenuRepository menuRepository;
    private final MenuReaderService readerService;
    private final I18nService i18nService;
    private final AuthoritiesSupport authoritiesSupport;
    private final List<MenuItemsResolver> resolvers;

    private final RecordsService recordsService;

    private final List<Consumer<MenuDto>> onChangeListeners = new CopyOnWriteArrayList<>();

    public long getLastModifiedTimeMs() {
        return menuRepository.getLastModifiedTime()
            .map(Instant::toEpochMilli)
            .orElse(0L);
    }

    public MenuDto upload(MenuDeployArtifact module) {

        MenuDto menuDto = readerService.readMenu(module.getData(), module.getFilename());
        MenuEntity entity = mapToEntity(menuDto);

        MenuDto result = mapToDto(menuRepository.save(entity));
        onChangeListeners.forEach(it -> it.accept(result));
        return result;
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

    public MenuDto getMenuForCurrentUser(Integer version) {

        String userName = SecurityContextHolder.getContext().getAuthentication().getName();

        MenuDto menu = findFirstByAuthorities(Collections.singletonList(userName), version)
            .orElseGet(() -> {

                Set<String> authToRequest = new HashSet<>(authoritiesSupport.getCurrentUserAuthorities());

                authToRequest.remove(userName);
                List<String> orderedAuthorities = getOrderedAuthorities(authToRequest);
                return findFirstByAuthorities(orderedAuthorities, version).orElse(null);
            });
        if (menu == null) {
            menu = findDefaultMenu(version);
        }
        return menu;
    }

    public ResolvedMenuDto getResolvedMenuForCurrentUser() {
        Set<String> authorities = new HashSet<>(authoritiesSupport.getCurrentUserAuthorities());
        MenuDto dto = getMenuForCurrentUser(null);
        return resolveMenu(dto, authorities);
    }

    private boolean compareVersion(Integer v0, Integer v1) {
        if (v0 == null) {
            v0 = 0;
        }
        if (v1 == null) {
            v1 = 0;
        }
        return v0.equals(v1);
    }

    public boolean isDefaultMenu(String id) {
        return id != null && DEFAULT_MENUS.contains(id);
    }

    private Optional<MenuDto> findFirstByAuthorities(List<String> authorities, Integer version) {
        return authorities.stream()
            .map(menuRepository::findAllByAuthoritiesContains)
            .flatMap(List::stream)
            .filter(menu -> compareVersion(menu.getVersion(), version))
            .filter(entity -> !isDefaultMenu(entity.getExtId()))
            .map(this::mapToDto)
            .findFirst();
    }

    private List<String> getOrderedAuthorities(Set<String> userAuthorities) {

        DataValue priorityArr = recordsService.getAtt(
            RecordRef.create(ConfigRecords.ID, "menu-group-priority"), "value?json");

        List<String> priority = new ArrayList<>();
        priorityArr.forEach(v -> {
            String id = v.get("id").asText();
            if (StringUtils.isNotBlank(id)) {
                priority.add(id);
            }
        });

        Set<String> allUserAuthorities = new HashSet<>(userAuthorities);
        allUserAuthorities.remove(DEFAULT_AUTHORITY);
        priority.retainAll(allUserAuthorities);
        allUserAuthorities.removeAll(priority);

        List<String> orderedAuthorities = new LinkedList<>();
        orderedAuthorities.addAll(priority);
        orderedAuthorities.addAll(allUserAuthorities);
        orderedAuthorities.add(DEFAULT_AUTHORITY);

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
            i18nService::getMessage,
            resolvers
        ).getResolvedMenu(menuDto);
    }

    public MenuDto save(MenuDto dto) {

        if (dto == null) {
            throw new IllegalArgumentException("Dto cannot be null");
        }

        if (StringUtils.isBlank(dto.getId())) {
            dto.setId(UUID.randomUUID().toString());
        }

        MenuDto result = mapToDto(menuRepository.save(mapToEntity(dto)));
        onChangeListeners.forEach(it -> it.accept(result));
        return result;
    }

    public void deleteByExtId(String menuId) {
        if (StringUtils.isBlank(menuId) || isDefaultMenu(menuId)) {
            return;
        }
        menuRepository.deleteByExtId(menuId);
    }

    public void addOnChangeListener(Consumer<MenuDto> listener) {
        onChangeListeners.add(listener);
    }

    public static class SubMenus extends HashMap<String, SubMenuDef> {
    }
}
