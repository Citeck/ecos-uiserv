package ru.citeck.ecos.uiserv.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import ru.citeck.ecos.apps.EcosAppsApiFactory;
import ru.citeck.ecos.apps.app.module.type.dashboard.DashboardModule;
import ru.citeck.ecos.apps.app.module.type.form.FormModule;
import ru.citeck.ecos.uiserv.domain.DashboardDto;
import ru.citeck.ecos.uiserv.service.dashdoard.DashboardEntityService;
import ru.citeck.ecos.uiserv.service.form.EcosFormModel;
import ru.citeck.ecos.uiserv.service.form.EcosFormService;

@Slf4j
@Configuration
public class EcosModulesConfig {

    private EcosFormService formService;
    private ObjectMapper mapper = new ObjectMapper();
    private EcosAppsApiFactory apiFactory;

    private DashboardEntityService dashboardEntityService;

    private boolean initialized = false;

    public EcosModulesConfig(EcosFormService formService,
                             DashboardEntityService dashboardEntityService,
                             EcosAppsApiFactory apiFactory) {
        this.apiFactory = apiFactory;
        this.formService = formService;
        this.dashboardEntityService = dashboardEntityService;
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {

        if (initialized) {
            return;
        }

        apiFactory.getModuleApi().onModulePublished(FormModule.class, this::deployForm);
        apiFactory.getModuleApi().onModulePublished(DashboardModule.class, this::deployDashboard);

        apiFactory.getModuleApi().onModuleDeleted(FormModule.class, this::deleteForm);
        apiFactory.getModuleApi().onModuleDeleted(DashboardModule.class, this::deleteDashboard);

        initialized = true;
    }

    public void deleteForm(String formId) {
        log.info("Form module deleted: " + formId);
        formService.delete(formId);
    }

    public void deployForm(FormModule formModule) {
        log.info("Form module received: " + formModule.getId() + " " + formModule.getFormKey());
        //todo: remove conversion
        EcosFormModel formModel = mapper.convertValue(formModule, EcosFormModel.class);
        formService.save(formModel);
    }

    public void deleteDashboard(String dashboardId) {
        log.info("Dashboard module deleted: " + dashboardId);
        dashboardEntityService.delete(dashboardId);
    }

    public void deployDashboard(DashboardModule dashboardModule) {
        log.info("Dashboard module received: " + dashboardModule.getId() + " " + dashboardModule.getKey());
        //todo: remove conversion
        DashboardDto dashboardDTO = mapper.convertValue(dashboardModule, DashboardDto.class);
        dashboardEntityService.update(dashboardDTO);
    }
}
