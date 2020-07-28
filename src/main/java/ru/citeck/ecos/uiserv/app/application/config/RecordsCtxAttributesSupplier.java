package ru.citeck.ecos.uiserv.app.application.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.spring.web.rest.ContextAttributesSupplier;
import ru.citeck.ecos.records2.spring.web.rest.RecordsRestApi;
import ru.citeck.ecos.uiserv.app.security.service.SecurityUtils;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RecordsCtxAttributesSupplier implements ContextAttributesSupplier {

    private final RecordsRestApi recordsRestApi;

    @PostConstruct
    public void init () {
        recordsRestApi.registerContextAttsSupplier(this);
    }

    @Override
    public Map<String, Object> getAttributes() {

        Map<String, Object> model = new HashMap<>();
        String requestUsername = SecurityUtils.getCurrentUserLoginFromRequestContext();
        model.put("user", RecordRef.valueOf("alfresco/people@" + requestUsername));
        model.put("alfMeta", RecordRef.valueOf("alfresco/meta@"));

        return model;
    }
}
