package ru.citeck.ecos.uiserv.domain.menu.api.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.citeck.ecos.uiserv.domain.menu.service.MenuService;
import ru.citeck.ecos.uiserv.domain.menu.service.resolving.ResolvedMenuDto;

@RestController
@RequestMapping("/api/usermenu")
@Transactional
@RequiredArgsConstructor
public class MenuApi {
    private final MenuService menuService;

    @GetMapping
    public ResponseEntity<ResolvedMenuDto> getUserMenu(@ModelAttribute("username") String username) {
        if (username == null) {
            throw new IllegalArgumentException("Expecting username");
        }

        ResolvedMenuDto menu = menuService.getMenuForUser(username);

        return ResponseEntity.ok()
            .header("ecos-uiserv-menu-version", "0.1")
            .body(menu);
    }
}
