package ru.citeck.ecos.uiserv.service.userconfig;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import ru.citeck.ecos.commons.data.DataValue;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.UserConfigurationDto;
import ru.citeck.ecos.uiserv.domain.UserConfigurationEntity;
import ru.citeck.ecos.uiserv.repository.UserConfigurationsRepository;

import java.time.Instant;
import java.util.List;

import static org.junit.Assert.*;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class UserConfigurationsServiceTest {
    private static final String REQUEST_USERNAME_ATTRIBUTE = "requestUsername";
    private static final String USER_NAME = "username";

    @Value("${max-user-configurations-persisted}")
    private Integer limit;

    @Autowired
    private UserConfigurationsRepository userConfigurationsRepository;

    @Autowired
    private UserConfigurationsService userConfigurationsService;

    @Before
    public void prepare() {
        userConfigurationsRepository.deleteAll();
        setUserInContext();
    }

    private void setUserInContext() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            requestAttributes.setAttribute(REQUEST_USERNAME_ATTRIBUTE, USER_NAME, RequestAttributes.SCOPE_REQUEST);
        }
    }

    @Test
    public void save_persistenceTest() {
        String data = "{}";

        UserConfigurationDto configuration = new UserConfigurationDto();
        configuration.setData(new DataValue(data));

        userConfigurationsService.save(configuration);

        List<UserConfigurationEntity> dtos = userConfigurationsRepository.findAll();

        assertEquals(1, dtos.size());

        UserConfigurationEntity dto = dtos.get(0);
        assertNotNull(dto.getId());
        assertTrue(StringUtils.isNotBlank(dto.getExternalId()));
        assertEquals(data, dto.getData());
    }

    @Test
    public void save_returningProperDto() {
        String data = "{\"data\":\"data\"}";

        UserConfigurationDto configuration = new UserConfigurationDto();
        configuration.setData(new DataValue(data));

        UserConfigurationDto saved = userConfigurationsService.save(configuration);

        assertTrue(StringUtils.isNotBlank(saved.getId()));
        assertEquals(configuration.getData(), saved.getData());
    }

    @Test(expected = IllegalArgumentException.class)
    public void save_nullEntity() {
        userConfigurationsService.save(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void save_entityWithNullData() {
        UserConfigurationDto configuration = new UserConfigurationDto();

        userConfigurationsService.save(configuration);
    }

    @Test
    public void save_usernameFromContextSet() {
        UserConfigurationDto configuration = new UserConfigurationDto();
        configuration.setData(new DataValue("data"));

        UserConfigurationDto saved = userConfigurationsService.save(configuration);

        assertEquals(USER_NAME, saved.getUserName());
    }

    public void save_creationTimeSet() {
        UserConfigurationDto configuration = new UserConfigurationDto();
        configuration.setData(new DataValue("data"));

        UserConfigurationDto saved = userConfigurationsService.save(configuration);

        assertNotNull(saved.getCreationTime());
    }

    @Test
    public void save_removingLatest() {
        Instant creationTime = Instant.now();
        String data = "{}";
        String name = "username";

        UserConfigurationDto latestDto = new UserConfigurationDto();
        latestDto.setCreationTime(creationTime);
        latestDto.setUserName(name);
        latestDto.setData(new DataValue(data));

        UserConfigurationDto savedLatestDto = userConfigurationsService.save(latestDto);
        String latestDtoExternalId = savedLatestDto.getId();

        for (int i = 0; i < limit; i++) {
            UserConfigurationDto configuration = new UserConfigurationDto();
            configuration.setCreationTime(Instant.now());
            configuration.setUserName(name);
            configuration.setData(new DataValue(data));

            userConfigurationsService.save(configuration);
        }

        assertEquals((int) limit, userConfigurationsRepository.countByUserName(name));
        assertNull(userConfigurationsRepository.findByExternalId(latestDtoExternalId));
    }

    @Test
    public void findByExternalId_findExistent() {
        Instant creationTime = Instant.now();
        String data = "{}";
        String name = "username";

        UserConfigurationDto configuration = new UserConfigurationDto();
        configuration.setCreationTime(creationTime);
        configuration.setUserName(name);
        configuration.setData(new DataValue(data));
        String persistentExternalId = userConfigurationsService.save(configuration).getId();

        UserConfigurationDto found = userConfigurationsService.findByExternalId(persistentExternalId);

        assertEquals(1, userConfigurationsRepository.countByUserName(name));
        assertNotNull(found);
        assertEquals(persistentExternalId, found.getId());
    }

    @Test
    public void findByExternalId_findNonexistent() {
        String persistentExternalId = "nonExistent";

        UserConfigurationDto found = userConfigurationsService.findByExternalId(persistentExternalId);

        assertNull(found);
    }
}
