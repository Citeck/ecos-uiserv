package ru.citeck.ecos.uiserv.domain.userconfig.service;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.userconfig.dto.UserConfigurationDto;
import ru.citeck.ecos.uiserv.domain.userconfig.repo.UserConfigurationEntity;
import ru.citeck.ecos.uiserv.domain.userconfig.repo.UserConfigurationsRepository;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class UserConfigurationsServiceTest {

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
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(USER_NAME, null, Collections.emptyList())
        );
    }

    @Test
    public void save_persistenceTest() {
        String data = "{}";

        UserConfigurationDto configuration = new UserConfigurationDto();
        configuration.setData(ObjectData.create(data));

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
        configuration.setData(ObjectData.create(data));

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
        configuration.setData(ObjectData.create("{}"));

        UserConfigurationDto saved = userConfigurationsService.save(configuration);

        assertEquals(USER_NAME, saved.getUserName());
    }

    public void save_creationTimeSet() {
        UserConfigurationDto configuration = new UserConfigurationDto();
        configuration.setData(ObjectData.create("data"));

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
        latestDto.setData(ObjectData.create(data));

        UserConfigurationDto savedLatestDto = userConfigurationsService.save(latestDto);
        String latestDtoExternalId = savedLatestDto.getId();

        for (int i = 0; i < limit; i++) {
            UserConfigurationDto configuration = new UserConfigurationDto();
            configuration.setCreationTime(Instant.now());
            configuration.setUserName(name);
            configuration.setData(ObjectData.create(data));

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
        configuration.setData(ObjectData.create(data));
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
