package ru.citeck.ecos.uiserv.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.uiserv.service.form.EcosFormService;
import ru.citeck.ecos.uiserv.service.form.EcosFormServiceImpl;
import ru.citeck.ecos.uiserv.service.form.FormProvider;
import ru.citeck.ecos.uiserv.service.form.NormalFormProvider;

import java.util.Collection;

@Configuration
public class FormRecordsServiceConfig extends RecordsServiceFactory {

    @Bean
    public EcosFormService formService(NormalFormProvider normalFormProvider,
                                       Collection<FormProvider> providers,
                                       RecordsService outgoingRecordsService) {
        final EcosFormServiceImpl result = new EcosFormServiceImpl();
        result.setNewFormsStore(normalFormProvider);
        result.setRecordsService(outgoingRecordsService);
        providers.forEach(result::register);
        return result;
    }
}
