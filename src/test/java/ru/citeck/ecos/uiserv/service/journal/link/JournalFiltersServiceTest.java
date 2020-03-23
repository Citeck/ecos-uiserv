package ru.citeck.ecos.uiserv.service.journal.link;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.JournalFilter;
import ru.citeck.ecos.uiserv.repository.JournalFilterRepository;

import java.time.Instant;
import java.util.List;

import static org.junit.Assert.*;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class JournalFiltersServiceTest {

    @Autowired
    private JournalFilterRepository journalFilterRepository;

    @Autowired
    private JournalFiltersService journalFiltersService;

    @Before
    public void cleanRepository() {
        journalFilterRepository.deleteAll();
    }

    @Test
    public void save_persistenceTest() {
        Instant creationTime = Instant.now();
        String data = "{}";
        String name = "username";

        JournalFilter filter = new JournalFilter();
        filter.setCreationTime(creationTime);
        filter.setUserName(name);
        filter.setData(data);

        journalFiltersService.save(filter);

        List<JournalFilter> dtos = journalFilterRepository.findAll();

        assertEquals(1, dtos.size());

        JournalFilter dto = dtos.get(0);
        assertNotNull(dto.getId());
        assertTrue(StringUtils.isNotBlank(dto.getExternalId()));
        assertEquals(creationTime, dto.getCreationTime());
        assertEquals(name, dto.getUserName());
        assertEquals(data, dto.getData());
    }

    @Test
    public void save_removingLatest() {
        Instant creationTime = Instant.now();
        String data = "{}";
        String name = "username";

        JournalFilter latest = new JournalFilter();
        latest.setCreationTime(creationTime);
        latest.setUserName(name);
        latest.setData(data);

        JournalFilter latestDto = journalFiltersService.save(latest);
        String latestDtoExternalId = latestDto.getExternalId();

        for (int i = 0; i < 1000; i++) {
            JournalFilter filter = new JournalFilter();
            filter.setCreationTime(Instant.now());
            filter.setUserName(name);
            filter.setData(data);

            journalFiltersService.save(filter);
        }


        assertEquals(1000, journalFilterRepository.countByUserName(name));
        assertNull(journalFilterRepository.findByExternalId(latestDtoExternalId));
    }

    @Test
    public void findByExternalId_findExistent() {
        Instant creationTime = Instant.now();
        String data = "{}";
        String name = "username";

        JournalFilter filter = new JournalFilter();
        filter.setCreationTime(creationTime);
        filter.setUserName(name);
        filter.setData(data);
        String persistentExternalId = journalFiltersService.save(filter).getExternalId();

        JournalFilter found = journalFiltersService.findByExternalId(persistentExternalId);

        assertEquals(1, journalFilterRepository.countByUserName(name));
        assertNotNull(found);
        assertEquals(persistentExternalId, found.getExternalId());
    }

    @Test
    public void findByExternalId_findNonexistent() {
        String persistentExternalId = "nonExistent";

        JournalFilter found = journalFiltersService.findByExternalId(persistentExternalId);

        assertEquals(0, journalFilterRepository.count());
        assertNull(found);
    }

    @Test
    public void deleteByExternalId_deleteExistent() {
        Instant creationTime = Instant.now();
        String data = "{}";
        String name = "username";

        JournalFilter filter = new JournalFilter();
        filter.setCreationTime(creationTime);
        filter.setUserName(name);
        filter.setData(data);
        String persistentExternalId = journalFiltersService.save(filter).getExternalId();

        journalFiltersService.deleteByExternalId(persistentExternalId);

        assertEquals(0, journalFilterRepository.count());
    }

    @Test
    public void deleteByExternalId_deleteNonexistent() {
        String persistentExternalId = "nonExistent";

        journalFiltersService.deleteByExternalId(persistentExternalId);

        assertEquals(0, journalFilterRepository.count());
    }
}
