package ru.citeck.ecos.uiserv.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import ru.citeck.ecos.apps.module.type.impl.dashboard.DashboardModule;
import ru.citeck.ecos.apps.module.type.impl.form.FormModule;
import ru.citeck.ecos.apps.queue.EcosAppQueues;
import ru.citeck.ecos.apps.queue.ModulePublishMsg;
import ru.citeck.ecos.apps.queue.ModulePublishResultMsg;
import ru.citeck.ecos.uiserv.domain.DashboardDTO;
import ru.citeck.ecos.uiserv.service.dashdoard.DashboardEntityService;
import ru.citeck.ecos.uiserv.service.form.EcosFormModel;
import ru.citeck.ecos.uiserv.service.form.EcosFormService;

@Slf4j
@Configuration
public class EcosModulesConfig {

    private EcosFormService formService;
    private ObjectMapper mapper = new ObjectMapper();
    private AmqpTemplate amqpTemplate;

    private DashboardEntityService dashboardEntityService;

    public EcosModulesConfig(EcosFormService formService,
                             AmqpTemplate amqpTemplate,
                             DashboardEntityService dashboardEntityService) {
        this.formService = formService;
        this.amqpTemplate = amqpTemplate;
        this.dashboardEntityService = dashboardEntityService;
    }

    @RabbitListener(queues = {EcosAppQueues.MODULE_TYPE_PUBLISH_PREFIX + FormModule.TYPE})
    public void deployForm(ModulePublishMsg msg) {

        ModulePublishResultMsg result = new ModulePublishResultMsg();
        result.setRevId(msg.getRevId());

        try {

            byte[] formData = msg.getData();

            if (formData == null) {
                throw new RuntimeException("Form can't be deployed. Data is not received");
            }

            EcosFormModel formModel = mapper.readValue(formData, EcosFormModel.class);
            formService.save(formModel);

            result.setSuccess(true);

        } catch (Exception e) {

            result.setMsg(e.getMessage());
            result.setSuccess(false);
        }

        amqpTemplate.convertAndSend(EcosAppQueues.PUBLISH_RESULT_ID, result);
    }

    @RabbitListener(queues = {EcosAppQueues.MODULE_TYPE_PUBLISH_PREFIX + DashboardModule.TYPE})
    public void deployDashboard(ModulePublishMsg msg) {

        ModulePublishResultMsg result = new ModulePublishResultMsg();
        result.setRevId(msg.getRevId());

        try {

            byte[] dashboardData = msg.getData();

            if (dashboardData == null) {
                throw new RuntimeException("Dashboard can't be deployed. Data is not received");
            }

            DashboardDTO dashboardDTO = mapper.readValue(dashboardData, DashboardDTO.class);
            dashboardEntityService.update(dashboardDTO);

            result.setSuccess(true);

        } catch (Exception e) {

            result.setMsg(e.getMessage());
            result.setSuccess(false);
        }

        amqpTemplate.convertAndSend(EcosAppQueues.PUBLISH_RESULT_ID, result);
    }
}
