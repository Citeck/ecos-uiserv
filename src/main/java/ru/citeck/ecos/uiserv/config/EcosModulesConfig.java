package ru.citeck.ecos.uiserv.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import ru.citeck.ecos.apps.EcosAppsApiFactory;
import ru.citeck.ecos.apps.app.module.type.dashboard.DashboardModule;
import ru.citeck.ecos.apps.app.module.type.form.FormModule;
import ru.citeck.ecos.uiserv.domain.DashboardDto;
import ru.citeck.ecos.uiserv.service.dashdoard.DashboardEntityService;
import ru.citeck.ecos.uiserv.service.form.EcosFormModel;
import ru.citeck.ecos.uiserv.service.form.EcosFormService;

import javax.annotation.PostConstruct;

@Slf4j
@Configuration
public class EcosModulesConfig {

    private EcosFormService formService;
    private ObjectMapper mapper = new ObjectMapper();
    private EcosAppsApiFactory apiFactory;

    private DashboardEntityService dashboardEntityService;

    public EcosModulesConfig(EcosFormService formService,
                             DashboardEntityService dashboardEntityService,
                             EcosAppsApiFactory apiFactory) {
        this.apiFactory = apiFactory;
        this.formService = formService;
        this.dashboardEntityService = dashboardEntityService;
    }

    @PostConstruct
    public void init() {
        apiFactory.getModuleApi().onModulePublished(FormModule.class, this::deployForm);
        apiFactory.getModuleApi().onModulePublished(DashboardModule.class, this::deployDashboard);
    }

    public void deployForm(FormModule formModule) {
        log.info("Form module received: " + formModule.getId() + " " + formModule.getFormKey());
        //todo: remove conversion
        EcosFormModel formModel = mapper.convertValue(formModule, EcosFormModel.class);
        formService.save(formModel);
    }

    public void deployDashboard(DashboardModule dashboardModule) {
        log.info("Dashboard module received: " + dashboardModule.getId() + " " + dashboardModule.getKey());
        //todo: remove conversion
        DashboardDto dashboardDTO = mapper.convertValue(dashboardModule, DashboardDto.class);
        dashboardEntityService.update(dashboardDTO);
    }
}
