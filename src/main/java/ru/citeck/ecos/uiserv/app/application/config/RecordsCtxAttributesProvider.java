package ru.citeck.ecos.uiserv.app.application.config;

import kotlin.jvm.functions.Function0;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.context.lib.auth.AuthContext;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.record.request.ctxatts.CtxAttsProvider;
import ru.citeck.ecos.webapp.api.context.EcosWebAppContext;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RecordsCtxAttributesProvider implements CtxAttsProvider {

    private final EcosWebAppContext webAppContext;

    @Override
    public void fillContextAtts(@NotNull Map<String, Object> map) {
        String requestUsername = AuthContext.getCurrentUser();
        map.put("user", RecordRef.valueOf("emodel/person@" + requestUsername));
        map.put("alfMeta", (Function0<Object>) this::getAlfMeta);
    }

    Object getAlfMeta() {
        if (webAppContext.getWebAppsApi().isAppAvailable("alfresco")) {
            return RecordRef.valueOf("alfresco/meta@");
        } else {
            return null;
        }
    }

    @Override
    public float getOrder() {
        return 0;
    }
}
