package ru.citeck.ecos.uiserv.web.rest.v1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.citeck.ecos.uiserv.config.ApplicationProperties;
import ru.citeck.ecos.uiserv.service.AuthoritiesSupport;
import ru.citeck.ecos.uiserv.service.i18n.I18nService;
import ru.citeck.ecos.uiserv.service.menu.MenuService;
import ru.citeck.ecos.uiserv.web.rest.menu.dto.Menu;
import ru.citeck.ecos.uiserv.web.rest.menu.dto.MenuFactory;
import ru.citeck.ecos.uiserv.web.rest.menu.resolvers.MenuItemsResolver;
import ru.citeck.ecos.uiserv.web.rest.menu.xml.MenuConfig;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/usermenu")
@Transactional
public class MenuApi {
    @Autowired
    private List<MenuItemsResolver> resolvers;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private MenuService menuService;

    @Autowired
    private AuthoritiesSupport authoritiesSupport;

    @Autowired
    private I18nService i18nService;

    @Value("${application.menu.useFileSystemResources:}")
    private String fsResourcesRoot;

    @GetMapping
    public ResponseEntity<Menu> getUserMenu(@ModelAttribute("username") String username, Locale locale) {
        if (username == null)
            throw new IllegalArgumentException("Expecting username");
        final Set<String> allUserAuthorities = new HashSet<>(authoritiesSupport.queryUserAuthorities(username));

        final Menu menu = getOrderedAuthorities(allUserAuthorities, username)
            .stream()
            .map(this::getMenu)
            .filter(Optional::isPresent)
            .findFirst()
            .orElse(this.getMenu(DEFAULT_AUTHORITY))
            .map(menuView -> loadMenuFromStore(menuView, allUserAuthorities, locale))
            .orElseGet(() -> loadMenuFromClassPath(allUserAuthorities, locale));

        return ResponseEntity.ok()
            .header("ecos-uiserv-menu-version", "0.1")
            .body(menu);
    }

    private Menu loadMenuFromStore(MenuService.MenuView menuView, Set<String> allUserAuthorities,
                                   Locale locale) {
        return new MenuFactory(allUserAuthorities,
            key -> i18nService.getMessage(key),
            resolvers)
            .getResolvedMenu(menuView.xml);
    }

    private Optional<MenuService.MenuView> getMenu(String authority) {
        return menuService.getMenu(authority + "-menu");
    }

    private static final String DEFAULT_AUTHORITY = "default";

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

    private Menu loadMenuFromClassPath(Set<String> allUserAuthorities, Locale locale) {
        final MenuConfig menuConfig;

        final Resource menuConfigResource = getResource("/menu/default-menu.xml");
        try {
            final JAXBContext jaxbContext = JAXBContext.newInstance(MenuConfig.class);
            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            try (InputStream is = menuConfigResource.getInputStream()) {
                menuConfig = (MenuConfig) unmarshaller.unmarshal(is);
            }
        } catch (JAXBException | IOException e) {
            throw new RuntimeException(e);
        }

        return new MenuFactory(allUserAuthorities,
            key -> i18nService.getMessage(key),
            resolvers).getResolvedMenu(menuConfig);
    }

    private Resource getResource(String relativePath) {
        return fsResourcesRoot.equals("") ? new ClassPathResource(relativePath) :
            new FileSystemResource(fsResourcesRoot + relativePath);
    }
}
