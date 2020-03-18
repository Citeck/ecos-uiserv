package ru.citeck.ecos.uiserv.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.uiserv.domain.DashboardDto;
import ru.citeck.ecos.uiserv.service.action.ActionService;
import ru.citeck.ecos.uiserv.service.dashdoard.DashboardService;
import ru.citeck.ecos.uiserv.service.form.EcosFormModel;
import ru.citeck.ecos.uiserv.service.form.EcosFormService;

//@Slf4j
//@Configuration
//@RequiredArgsConstructor
public class EcosModulesConfig {

    private ObjectMapper mapper = new ObjectMapper();

    //private final EcosFormService formService;
    //private final EcosAppsApiFactory apiFactory;
    //private final DashboardService dashboardService;
    //private final ActionService actionService;

    private boolean initialized = false;

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {

        if (initialized) {
            return;
        }
/*
        apiFactory.getModuleApi().onModulePublished(FormModule.class, this::deployForm);
        apiFactory.getModuleApi().onModulePublished(DashboardModule.class, this::deployDashboard);
        apiFactory.getModuleApi().onModulePublished(ActionModule.class, this::deployAction);

        apiFactory.getModuleApi().onModuleDeleted(FormModule.class, this::deleteForm);
        apiFactory.getModuleApi().onModuleDeleted(DashboardModule.class, this::deleteDashboard);
        apiFactory.getModuleApi().onModuleDeleted(ActionModule.class, this::deleteAction);*/

        initialized = true;
    }

/*    public void deployAction(ActionModule actionModule) {
        actionService.updateAction(actionModule);
    }

    public void deleteAction(String actionId) {
        actionService.deleteAction(actionId);
    }

    public void deleteForm(String formId) {
        log.info("Form module deleted: " + formId);
        formService.delete(formId);
    }

    public void deployForm(FormModule formModule) {
        log.info("Form module received: " + formModule.getId() + " " + formModule.getFormKey());
        EcosFormModel formModel = mapper.convertValue(formModule, EcosFormModel.class);
        formService.save(formModel);
    }

    public void deleteDashboard(String dashboardId) {
        log.info("Dashboard module deleted: " + dashboardId);
        dashboardService.removeDashboard(dashboardId);
    }

    public void deployDashboard(DashboardModule module) {

        log.info("Dashboard module received: " + module.getId() + " " + module.getTypeRef());

        DashboardDto dto = new DashboardDto();
        dto.setPriority(module.getPriority());
        dto.setId(module.getId());
        dto.setConfig(module.getConfig());
        dto.setTypeRef(RecordRef.create("emodel", "type", module.getTypeRef().getId()));
        dto.setAuthority(module.getAuthority());

        dashboardService.saveDashboard(dto);
    }*/
}
