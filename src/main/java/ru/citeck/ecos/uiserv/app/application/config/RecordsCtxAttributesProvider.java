package ru.citeck.ecos.uiserv.app.application.config;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.record.request.ctxatts.CtxAttsProvider;
import ru.citeck.ecos.uiserv.app.security.service.SecurityUtils;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RecordsCtxAttributesProvider implements CtxAttsProvider {

    @Override
    public void fillContextAtts(@NotNull Map<String, Object> map) {
        String requestUsername = SecurityUtils.getCurrentUserLoginFromRequestContext();
        map.put("user", RecordRef.valueOf("alfresco/people@" + requestUsername));
        map.put("alfMeta", RecordRef.valueOf("alfresco/meta@"));
    }

    @Override
    public float getOrder() {
        return 0;
    }
}
