package ru.citeck.ecos.uiserv.domain.theme_old.api.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.citeck.ecos.uiserv.domain.theme.service.ThemeService;

@RestController
@RequestMapping("/api/themes")
@Transactional
public class ThemeApi {

    @Autowired
    private ThemeService themeService;

    @GetMapping("/current")
    public String getUserTheme(@RequestParam(required = false, name = "siteId") String siteId) {
        return themeService.getActiveTheme();
    }
}
