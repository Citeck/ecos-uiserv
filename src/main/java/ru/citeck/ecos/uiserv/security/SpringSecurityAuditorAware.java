package ru.citeck.ecos.uiserv.security;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.uiserv.config.Constants;

import java.util.Optional;

/**
 * Implementation of AuditorAware based on Spring Security.
 */
@Component
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    @NotNull
    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.of(Optional.ofNullable(SecurityUtils.getCurrentUserLoginFromRequestContext())
            .orElse(Constants.ANONYMOUS_USER));
    }
}
