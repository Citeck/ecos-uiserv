package ru.citeck.ecos.uiserv.app.application.config;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.spring.config.RecordsServiceFactoryConfiguration;
import ru.citeck.ecos.uiserv.app.security.service.SecurityUtils;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RecordsCtxAttributesSupplier {

    private final RecordsServiceFactoryConfiguration recordsServiceFactory;

    @PostConstruct
    public void init () {
        recordsServiceFactory.setCustomDefaultCtxAttsProvider(this::getAttributes);
    }

    @NotNull
    public Map<String, Object> getAttributes() {

        Map<String, Object> model = new HashMap<>();
        String requestUsername = SecurityUtils.getCurrentUserLoginFromRequestContext();
        model.put("user", RecordRef.valueOf("alfresco/people@" + requestUsername));
        model.put("alfMeta", RecordRef.valueOf("alfresco/meta@"));

        return model;
    }
}
