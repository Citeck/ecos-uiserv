package ru.citeck.ecos.uiserv.app.common.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AuthoritiesSupport {

    public List<String> getCurrentUserAuthorities() {

        Authentication authentication = getAuth();
        if (authentication == null) {
            return Collections.emptyList();
        }

        return authentication.getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
    }

    private Authentication getAuth() {

        SecurityContext context = SecurityContextHolder.getContext();

        if (context == null) {
            return null;
        }
        return context.getAuthentication();
    }
}
