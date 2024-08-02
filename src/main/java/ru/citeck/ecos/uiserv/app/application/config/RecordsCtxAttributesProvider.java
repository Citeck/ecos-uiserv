package ru.citeck.ecos.uiserv.app.application.config;

import kotlin.jvm.functions.Function0;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.context.lib.auth.AuthContext;
import ru.citeck.ecos.records3.record.request.ctxatts.CtxAttsProvider;
import ru.citeck.ecos.webapp.api.EcosWebAppApi;
import ru.citeck.ecos.webapp.api.constants.AppName;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RecordsCtxAttributesProvider implements CtxAttsProvider {

    private final EcosWebAppApi webAppContext;

    @Override
    public void fillContextAtts(@NotNull Map<String, Object> map) {
        String requestUsername = AuthContext.getCurrentUser();
        map.put("user", EntityRef.valueOf("emodel/person@" + requestUsername));
        map.put("alfMeta", (Function0<Object>) this::getAlfMeta);
    }

    Object getAlfMeta() {
        if (webAppContext.getRemoteWebAppsApi().isAppAvailable(AppName.ALFRESCO)) {
            return EntityRef.valueOf("alfresco/meta@");
        } else {
            return null;
        }
    }

    @Override
    public float getOrder() {
        return 0;
    }
}
