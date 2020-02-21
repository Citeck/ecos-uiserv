package ru.citeck.ecos.uiserv.patch;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.apps.EappsFactory;
import ru.citeck.ecos.apps.app.EcosAppImpl;
import ru.citeck.ecos.apps.app.EcosAppVersion;
import ru.citeck.ecos.apps.app.module.ModuleRef;
import ru.citeck.ecos.apps.app.module.type.ui.dashboard.DashboardModule;
import ru.citeck.ecos.metarepo.EcosMetaRepo;
import ru.citeck.ecos.uiserv.domain.OldDashboardDto;
import ru.citeck.ecos.uiserv.service.dashdoard.DashboardEntityService;

import javax.annotation.PostConstruct;
import java.util.*;

@Slf4j
@Component
@DependsOn("liquibase")
@RequiredArgsConstructor
public class MigrateDashboardsPatch {

    private static final String PATCH_KEY = "patch.MigrateDashboardsPatch_v4";
    private static final String KEY_TYPE_PREFIX = "type_";

    private final EcosMetaRepo metaRepo;
    private final DashboardEntityService dashboardEntityService;
    private final EappsFactory eappsFactory;

    @PostConstruct
    @Transactional
    public void execute() {

        PatchStatus currentStatus = metaRepo.get(PATCH_KEY, PatchStatus.class);
        if (currentStatus != null) {
            return;
        }

        PatchStatus status = new PatchStatus();

        Map<String, DashboardModule> modules = new HashMap<>();

        List<OldDashboardDto> dashboards = dashboardEntityService.getAll();
        status.total = dashboards.size();

        dashboards.forEach(d -> {

            String key = d.getKey();
            boolean wasAdded = false;

            try {

                if (key != null) {

                    DashboardModule module = new DashboardModule();
                    module.setId(UUID.randomUUID().toString());
                    module.setConfig(d.getConfig());
                    module.setAuthority(d.getUser());

                    if (key.startsWith(KEY_TYPE_PREFIX)) {

                        key = key.substring(KEY_TYPE_PREFIX.length());
                        ModuleRef typeRef = ModuleRef.create("model/type", key);

                        module.setTypeRef(typeRef);

                        modules.put(module.getId(), module);
                        wasAdded = true;

                    } else if (key.equals("DEFAULT")) {

                        String type = d.getType();

                        if ("user-dashboard".equals(type)) {

                            module.setId("user-dashboard");
                            module.setTypeRef(ModuleRef.create("model/type", "user-dashboard"));
                            modules.put(module.getId(), module);
                            wasAdded = true;

                        } else if ("case-details".equals(type)) {

                            module.setTypeRef(ModuleRef.create("model/type", "base"));
                            module.setId("base-type-dashboard");
                            modules.put(module.getId(), module);
                            wasAdded = true;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Dashboard processing exception", e);
            }
            if (!wasAdded) {
                status.skippedDashboards.add(key + " " + d.getType());
                status.skipped++;
            }
        });

        modules.values().forEach(m -> status.migrated.add(m.getId() + " (" + m.getTypeRef() + ")"));

        status.deployed = modules.size();

        EcosAppImpl app = new EcosAppImpl();
        app.setVersion(new EcosAppVersion("1.0.0"));
        app.setId("uiserv-dashboard-patch");
        app.setModules(new ArrayList<>(modules.values()));
        app.setName("uiserv-dashboard-patch");
        app.setSystem(true);

        eappsFactory.getAppsApiFactory().getAppApi().deployApp("mutation", app);

        metaRepo.put(PATCH_KEY, status);
    }

    @Data
    public static class PatchStatus {

        private List<String> migrated = new ArrayList<>();
        private List<String> skippedDashboards = new ArrayList<>();
        private int skipped = 0;
        private int deployed = 0;
        private int total = 0;
    }
}
