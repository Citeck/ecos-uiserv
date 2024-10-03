package ru.citeck.ecos.uiserv.domain.menu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.config.lib.records.CfgRecordsDao;
import ru.citeck.ecos.context.lib.auth.AuthContext;
import ru.citeck.ecos.model.lib.workspace.WorkspaceService;
import ru.citeck.ecos.records2.predicate.model.Predicate;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy;
import ru.citeck.ecos.uiserv.domain.menu.dao.MenuDao;
import ru.citeck.ecos.uiserv.domain.menu.repo.MenuEntity;
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDeployArtifact;
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDto;
import ru.citeck.ecos.uiserv.domain.menu.dto.SubMenuDef;
import ru.citeck.ecos.uiserv.domain.menu.service.format.MenuReaderService;

import jakarta.annotation.PostConstruct;
import ru.citeck.ecos.webapp.api.constants.AppName;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MenuService {

    private static final String DEFAULT_AUTHORITY = "GROUP_EVERYONE";
    private static final String DEFAULT_MENU_ID = "default-menu";
    private static final String DEFAULT_MENU_V1_ID = "default-menu-v1";
    private static final String DEFAULT_PERSONAL_WS_MENU_ID = "default-personal-ws-menu";
    private static final String DEFAULT_WS_MENU_ID = "default-ws-menu";

    private final List<String> CONFIGS_TO_REMOVE = Collections.singletonList(
        "default-menu-for-admin-v1"
    );

    public static final List<String> DEFAULT_MENUS = Arrays.asList(
        DEFAULT_MENU_ID,
        DEFAULT_MENU_V1_ID,
        DEFAULT_PERSONAL_WS_MENU_ID,
        DEFAULT_WS_MENU_ID
    );

    private final MenuDao menuDao;
    private final MenuReaderService readerService;
    private final WorkspaceService workspaceService;

    private final RecordsService recordsService;

    private final List<BiConsumer<MenuDto, MenuDto>> onChangeListeners = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        CONFIGS_TO_REMOVE.forEach(cfg -> {
            MenuEntity menu = menuDao.findByExtId(cfg);
            if (menu != null) {
                MenuDto dtoToRemove = mapToDto(menu);
                log.info("Remove menu config: {}", Json.getMapper().toString(dtoToRemove));
                menuDao.delete(menu);
            }
        });
    }

    public long getLastModifiedTimeMs() {
        return Optional.of(menuDao.getLastModifiedTime())
            .map(Instant::toEpochMilli)
            .orElse(0L);
    }

    public MenuDto upload(MenuDeployArtifact module) {

        MenuDto menuDto = readerService.readMenu(module.getData(), module.getFilename());

        MenuDto menuBefore = null;
        MenuEntity entityBefore = menuDao.findByExtId(menuDto.getId());
        if (entityBefore != null) {
            menuBefore = mapToDto(entityBefore);
        }

        MenuEntity entity = mapToEntity(menuDto);
        MenuDto result = mapToDto(menuDao.save(entity));

        for (BiConsumer<MenuDto, MenuDto> listener : onChangeListeners) {
            listener.accept(menuBefore, result);
        }

        return result;
    }

    public Set<String> getAllAuthoritiesWithMenu() {
        return menuDao.getAllAuthoritiesWithMenu();
    }

    public long getCount(Predicate predicate) {
        return menuDao.getCount(predicate);
    }

    public List<MenuDto> findAll(Predicate predicate, int max, int skip, List<SortBy> sort) {
        return menuDao.findAll(predicate, max, skip, sort).stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    public List<MenuDto> getAllMenus() {
        return menuDao.findAll()
            .stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    public Optional<MenuDto> getMenu(String menuId) {
        if (StringUtils.isBlank(menuId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(menuDao.findByExtId(menuId)).map(this::mapToDto);
    }

    private MenuEntity mapToEntity(MenuDto menuDto) {

        MenuEntity entity = menuDao.findByExtId(menuDto.getId());
        if (entity == null) {
            entity = new MenuEntity();
            entity.setExtId(menuDto.getId());
        }

        entity.setTenant("");
        entity.setType(menuDto.getType());
        entity.setAuthorities(new ArrayList<>(menuDto.getAuthorities()));
        entity.setVersion(menuDto.getVersion());
        entity.setItems(Json.getMapper().toString(menuDto.getSubMenu()));
        entity.setWorkspace(menuDto.getWorkspaceRef().getLocalId());

        return entity;
    }

    private MenuDto mapToDto(MenuEntity entity) {

        if (entity == null) {
            return null;
        }

        EntityRef workspace = EntityRef.EMPTY;
        if (StringUtils.isNotBlank(entity.getWorkspace())) {
            workspace = EntityRef.create(AppName.EMODEL, "workspace", entity.getWorkspace());
        }

        return MenuDto.create()
            .withId(entity.getExtId())
            .withType(entity.getType())
            .withAuthorities(new ArrayList<>(entity.getAuthorities()))
            .withSubMenu(Json.getMapper().read(entity.getItems(), SubMenus.class))
            .withVersion(entity.getVersion())
            .withWorkspaceRef(workspace)
            .build();
    }

    public MenuDto getMenuForCurrentUser(Integer version) {
        return getMenuForCurrentUser(version, "");
    }

    public MenuDto getMenuForCurrentUser(Integer version, String workspace) {

        String userName = AuthContext.getCurrentUser();
        List<String> userNameVariants = Collections.singletonList(userName.toLowerCase());

        MenuDto menu = findFirstByAuthorities(userNameVariants, version, workspace)
            .orElseGet(() -> {

                Set<String> authToRequest = new HashSet<>(AuthContext.getCurrentUserWithAuthorities());

                authToRequest.removeAll(userNameVariants);
                List<String> orderedAuthorities = getOrderedAuthorities(authToRequest).stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

                return findFirstByAuthorities(orderedAuthorities, version, workspace).orElse(null);
            });
        if (menu == null) {
            menu = findDefaultMenu(version, workspace);
        }
        return menu;
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

    private Optional<MenuDto> findFirstByAuthorities(
        List<String> authorities,
        Integer version,
        String workspace
    ) {
        return authorities.stream()
            .map(it -> menuDao.findAllByAuthoritiesContains(it, workspace))
            .flatMap(List::stream)
            .filter(menu -> compareVersion(menu.getVersion(), version))
            .filter(entity -> !isDefaultMenu(entity.getExtId()))
            .map(this::mapToDto)
            .findFirst();
    }

    private List<String> getOrderedAuthorities(Set<String> userAuthorities) {

        DataValue priorityArr = recordsService.getAtt(
            EntityRef.create(CfgRecordsDao.ID, "menu-group-priority"), "value[]?json");

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
        priority.forEach(allUserAuthorities::remove);

        List<String> orderedAuthorities = new LinkedList<>();
        orderedAuthorities.addAll(priority);
        orderedAuthorities.addAll(allUserAuthorities);
        orderedAuthorities.add(DEFAULT_AUTHORITY);

        return orderedAuthorities;
    }

    private MenuDto findDefaultMenu(Integer version, String workspace) {
        int intVersion = version != null ? version : 0;
        Optional<MenuDto> result;
        if (workspace.isEmpty()) {
            if (intVersion == 1) {
                result = getMenu(DEFAULT_MENU_V1_ID);
            } else {
                result = getMenu(DEFAULT_MENU_ID);
            }
        } else if (workspace.startsWith("user$")) {
            result = getMenu(DEFAULT_PERSONAL_WS_MENU_ID);
        } else {
            result = getMenu(DEFAULT_WS_MENU_ID);
        }
        return result.orElseThrow(() -> new RuntimeException("Cannot load default menu. Version: " + version));
    }

    private void checkWorkspaceAccess(String workspaceId, boolean newMenu) {
        if (workspaceId.isEmpty() || AuthContext.isRunAsSystem()) {
            return;
        }
        var user = AuthContext.getCurrentRunAsUser();
        if (!workspaceService.isUserManagerOf(user, workspaceId)) {
            var action = newMenu ? "create" : "change";
            throw new RuntimeException("You can't " + action + " menu in workspace " + workspaceId);
        }
    }

    public MenuDto save(MenuDto dto) {

        if (dto == null) {
            throw new IllegalArgumentException("Dto cannot be null");
        }


        MenuDto valueBefore = null;
        MenuEntity entityBefore = menuDao.findByExtId(dto.getId());
        if (entityBefore != null) {
            checkWorkspaceAccess(entityBefore.getWorkspace(), false);
            valueBefore = mapToDto(entityBefore);
        }
        checkWorkspaceAccess(dto.getWorkspaceRef().getLocalId(), entityBefore == null);

        MenuDto result = mapToDto(menuDao.save(mapToEntity(dto)));
        for (BiConsumer<MenuDto, MenuDto> listener : onChangeListeners) {
            listener.accept(valueBefore, result);
        }
        return result;
    }

    public void deleteByExtId(String menuId) {
        if (StringUtils.isBlank(menuId) || isDefaultMenu(menuId)) {
            return;
        }
        menuDao.deleteByExtId(menuId);
    }

    public void addOnChangeListener(BiConsumer<MenuDto, MenuDto> listener) {
        onChangeListeners.add(listener);
    }

    public static class SubMenus extends HashMap<String, SubMenuDef> {
    }
}
