package ru.citeck.ecos.uiserv.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import ru.citeck.ecos.apps.module.type.impl.form.FormModule;
import ru.citeck.ecos.apps.queue.EcosAppQueues;
import ru.citeck.ecos.apps.queue.ModulePublishMsg;
import ru.citeck.ecos.apps.queue.ModulePublishResultMsg;
import ru.citeck.ecos.uiserv.service.form.EcosFormModel;
import ru.citeck.ecos.uiserv.service.form.EcosFormService;

@Slf4j
@Configuration
public class EcosModulesConfig {

    private RestTemplate restTemplate;
    private EcosFormService formService;
    private ObjectMapper mapper = new ObjectMapper();
    private AmqpTemplate amqpTemplate;

    public EcosModulesConfig(@Qualifier("microRestTemplate") RestTemplate restTemplate,
                             EcosFormService formService,
                             AmqpTemplate amqpTemplate) {
        this.formService = formService;
        this.restTemplate = restTemplate;
        this.amqpTemplate = amqpTemplate;
    }

    //@RabbitListener(queues = {EcosAppQueues.PUBLISH_PREFIX + FormModule.TYPE})
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
}
