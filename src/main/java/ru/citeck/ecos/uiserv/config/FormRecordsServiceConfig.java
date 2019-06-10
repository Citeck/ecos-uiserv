package ru.citeck.ecos.uiserv.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.citeck.ecos.predicate.PredicateService;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.RecordsServiceImpl;
import ru.citeck.ecos.records2.meta.RecordsMetaService;
import ru.citeck.ecos.records2.request.rest.RestHandler;
import ru.citeck.ecos.uiserv.service.form.EcosFormRecords;
import ru.citeck.ecos.uiserv.service.form.EcosFormService;
import ru.citeck.ecos.uiserv.service.form.EcosFormServiceImpl;
import ru.citeck.ecos.uiserv.service.form.FormProvider;
import ru.citeck.ecos.uiserv.service.form.NormalFormProvider;

import java.util.Collection;

@Configuration
@RequiredArgsConstructor
public class FormRecordsServiceConfig extends RecordsServiceFactory {
    private final EcosFormRecords dao;

    @Bean
    public PredicateService formPredicateService() {
        return super.createPredicateService();
    }

    @Bean
    public RecordsMetaService formRecordsMetaService() {
        return super.createRecordsMetaService();
    }

    @Bean
    public RestHandler formRestQueryHandler() {
        //This is a records service that we implement, not the one we consume!
        //So let's not make it a bean to avoid its discovery
        final RecordsService recordsService = new RecordsServiceImpl(createRecordsMetaService(), createRecordsResolver());
        recordsService.register(dao);

        return new RestHandler(recordsService);
    }

    @Configuration
    public static class NotRequiresRecordsDAO {
        @Bean
        public EcosFormService formService(NormalFormProvider normalFormProvider, Collection<FormProvider> providers,
                                           RecordsService outgoingRecordsService) {
            final EcosFormServiceImpl result = new EcosFormServiceImpl();
            result.setNewFormsStore(normalFormProvider);
            result.setRecordsService(outgoingRecordsService);
            providers.forEach(result::register);
            return result;
        }
    }
}
