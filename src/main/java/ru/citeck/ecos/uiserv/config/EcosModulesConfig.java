package ru.citeck.ecos.uiserv.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import ru.citeck.ecos.apps.EcosAppsApiFactory;
import ru.citeck.ecos.apps.app.module.api.ModulePublishMsg;
import ru.citeck.ecos.apps.app.module.type.impl.dashboard.DashboardModule;
import ru.citeck.ecos.apps.app.module.type.impl.form.FormModule;
import ru.citeck.ecos.uiserv.domain.DashboardDTO;
import ru.citeck.ecos.uiserv.service.dashdoard.DashboardEntityService;
import ru.citeck.ecos.uiserv.service.form.EcosFormModel;
import ru.citeck.ecos.uiserv.service.form.EcosFormService;

import javax.annotation.PostConstruct;
import java.io.IOException;

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
        apiFactory.getModuleApi().onModulePublished(FormModule.TYPE, this::deployForm);
        apiFactory.getModuleApi().onModulePublished(DashboardModule.TYPE, this::deployDashboard);
    }

    public void deployForm(ModulePublishMsg msg) throws IOException {

        byte[] formData = msg.getData();

        if (formData == null) {
            throw new RuntimeException("Form can't be deployed. Data is not received");
        }

        EcosFormModel formModel = mapper.readValue(formData, EcosFormModel.class);
        formService.save(formModel);
    }

    public void deployDashboard(ModulePublishMsg msg) throws IOException {

        byte[] dashboardData = msg.getData();

        if (dashboardData == null) {
            throw new RuntimeException("Dashboard can't be deployed. Data is not received");
        }

        DashboardDTO dashboardDTO = mapper.readValue(dashboardData, DashboardDTO.class);
        dashboardEntityService.update(dashboardDTO);
    }
}
