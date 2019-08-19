package ru.citeck.ecos.uiserv.config;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.source.dao.RecordsDAO;
import ru.citeck.ecos.records2.source.dao.remote.RemoteRecordsDAO;

import java.util.List;

@Log4j2
@Component
public class RecordsDAORegistrar {

    private RestTemplate alfrescoRestTemplate;

    @Autowired
    public RecordsDAORegistrar(RecordsService recordsService,
                               List<RecordsDAO> sources,
                               @Qualifier("alfrescoRestTemplate") RestTemplate alfrescoRestTemplate) {

        this.alfrescoRestTemplate = alfrescoRestTemplate;

        final RemoteRecordsDAO alfrescoDao = new RemoteRecordsDAO();
        alfrescoDao.setId(RecordsServiceConfig.RECORDS_DAO_ID);
        alfrescoDao.setRestConnection(this::alfrescoJsonPost);
        registerRecordsDAO(recordsService, alfrescoDao);


        sources.forEach(recordsService::register);
    }

    private void registerRecordsDAO(RecordsService recordsService, RecordsDAO recordsSource) {
        log.info("Register recordsDAO: " + recordsSource.getId());
        recordsService.register(recordsSource);
    }

    private <T> T alfrescoJsonPost(String url, Object req, Class<T> respType) {
        return alfrescoRestTemplate.postForObject(url, req, respType);
    }
}
