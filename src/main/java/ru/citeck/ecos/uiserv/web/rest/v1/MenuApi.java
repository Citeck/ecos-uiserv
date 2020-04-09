package ru.citeck.ecos.uiserv.web.rest.v1;

import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.utils.IOUtils;
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
import ru.citeck.ecos.uiserv.domain.MenuConfigurationDto;
import ru.citeck.ecos.uiserv.service.AuthoritiesSupport;
import ru.citeck.ecos.uiserv.service.menu.MenuConfigurationService;
import ru.citeck.ecos.uiserv.service.translation.TranslationService;
import ru.citeck.ecos.uiserv.web.rest.menu.dto.Menu;
import ru.citeck.ecos.uiserv.web.rest.menu.dto.MenuFactory;
import ru.citeck.ecos.uiserv.web.rest.menu.resolvers.MenuItemsResolver;
import ru.citeck.ecos.uiserv.web.rest.menu.xml.MenuConfig;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@RestController
@RequestMapping("/api/usermenu")
@Transactional
@RequiredArgsConstructor
public class MenuApi {
    private static final String DEFAULT_AUTHORITY = "default";

    private final List<MenuItemsResolver> resolvers;
    private final ApplicationProperties applicationProperties;
    private final TranslationService i18n;
    private final MenuConfigurationService menuService;
    private final AuthoritiesSupport authoritiesSupport;
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

    private Optional<MenuConfigurationDto> getMenu(String authority) {
        return menuService.getMenu(authority + "-menu");
    }

    private Menu loadMenuFromStore(MenuConfigurationDto menuConfigDto, Set<String> allUserAuthorities,
                                   Locale locale) {
        return new MenuFactory(allUserAuthorities,
            messageKey -> Optional.ofNullable(menuConfigDto.getLocalizedString(messageKey, locale)).orElse(messageKey),
            resolvers)
            .getResolvedMenu(menuConfigDto.getConfig());
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

        final ResourceBundle resourceBundle;
        final Resource resource = getResource(
            String.format("/menu/default-menu_%s.properties",
                locale.toLanguageTag().split("-", 2)[0]));
        try (InputStream input = resource.getInputStream()) {
            resourceBundle = i18n.toBundle(IOUtils.toByteArray(input));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new MenuFactory(allUserAuthorities,
            resourceBundle::getString,
            resolvers).getResolvedMenu(menuConfig);
    }

    private Resource getResource(String relativePath) {
        return fsResourcesRoot.equals("") ? new ClassPathResource(relativePath) :
            new FileSystemResource(fsResourcesRoot + relativePath);
    }
}
