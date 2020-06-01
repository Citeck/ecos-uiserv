package ru.citeck.ecos.uiserv.service.icon.dto;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ru.citeck.ecos.uiserv.Application;
import ru.citeck.ecos.uiserv.domain.IconEntity;
import ru.citeck.ecos.uiserv.repository.IconRepository;
import ru.citeck.ecos.uiserv.service.icon.IconService;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class IconServiceTest {
    @Autowired
    private IconRepository iconRepository;
    @Autowired
    private IconService iconService;

    @Before
    public void cleanRepo() {
        iconRepository.deleteAll();
    }

    @Test
    public void save_imgDto_properPersist() {
        String id = "id";
        String type = "img";
        String format = "png";
        byte[] data = RandomUtils.nextBytes(20);
        String dataString = Base64.getEncoder().encodeToString(data);

        IconDto dto = new IconDto();

        dto.setId(id);
        dto.setType(type);
        dto.setFormat(format);
        dto.setData(dataString);

        iconService.save(dto);

        List<IconEntity> entities = iconRepository.findAll();
        assertEquals(1, entities.size());

        IconEntity saved = entities.get(0);
        assertEquals(id, saved.getExtId());
        assertEquals(type, saved.getType());
        assertEquals(format, saved.getFormat());
        assertArrayEquals(data, saved.getData());
    }

    @Test
    public void save_imgDto_properReturn() {
        String id = "id";
        String type = "img";
        String format = "png";
        byte[] data = RandomUtils.nextBytes(20);
        String dataString = Base64.getEncoder().encodeToString(data);

        IconDto dto = new IconDto();

        dto.setId(id);
        dto.setType(type);
        dto.setFormat(format);
        dto.setData(dataString);

        IconDto saved = iconService.save(dto);
        assertNotNull(saved);
        assertEquals(id, saved.getId());
        assertEquals(type, saved.getType());
        assertEquals(format, saved.getFormat());
        assertEquals(dataString, saved.getData());
        assertNotNull(saved.getModified());
    }

    @Test
    public void save_faDto_properPersist() {
        String id = "id";
        String type = "fa";
        String dataString = "data";
        byte[] data = dataString.getBytes(StandardCharsets.UTF_8);

        IconDto dto = new IconDto();

        dto.setId(id);
        dto.setType(type);
        dto.setData(dataString);

        iconService.save(dto);

        List<IconEntity> entities = iconRepository.findAll();
        assertEquals(1, entities.size());

        IconEntity saved = entities.get(0);
        assertEquals(id, saved.getExtId());
        assertEquals(type, saved.getType());
        assertNull(saved.getFormat());
        assertArrayEquals(data, saved.getData());
    }

    @Test
    public void save_faDto_properReturn() {
        String id = "id";
        String type = "fa";
        String dataString = "data";

        IconDto dto = new IconDto();

        dto.setId(id);
        dto.setType(type);
        dto.setData(dataString);

        IconDto saved = iconService.save(dto);
        assertNotNull(saved);
        assertEquals(id, saved.getId());
        assertEquals(type, saved.getType());
        assertNull(saved.getFormat());
        assertEquals(dataString, saved.getData());
        assertNotNull(saved.getModified());
    }

    @Test
    public void save_faDtoWithModifiedFilled_modifiedPutByRepo() {
        Instant modified = Instant.now().minus(10, ChronoUnit.MINUTES);

        IconDto dto = new IconDto();

        dto.setId("id");
        dto.setType("fa");
        dto.setData("data");
        dto.setModified(modified);

        IconDto saved = iconService.save(dto);

        assertTrue(saved.getModified().isAfter(modified));
    }

    @Test
    public void save_imgDtoWithModifiedFilled_modifiedPutByRepo() {
        Instant modified = Instant.now().minus(10, ChronoUnit.MINUTES);

        IconDto dto = new IconDto();

        dto.setId("id");
        dto.setType("img");
        dto.setData("data");
        dto.setModified(modified);

        IconDto saved = iconService.save(dto);

        assertTrue(saved.getModified().isAfter(modified));
    }

    @Test
    public void findAllByType_findsProperly() {
        IconDto dto1 = new IconDto();
        dto1.setId("id1");
        dto1.setType("fa");
        dto1.setData("data1");

        IconDto dto2 = new IconDto();
        dto2.setId("id2");
        dto2.setType("fa");
        dto2.setData("data2");

        IconDto dto3 = new IconDto();
        dto3.setId("id3");
        dto3.setType("img");
        byte[] data = RandomUtils.nextBytes(20);
        String dataString = Base64.getEncoder().encodeToString(data);
        dto3.setData(dataString);

        iconService.save(dto1);
        iconService.save(dto2);
        iconService.save(dto3);

        List<IconDto> found = iconService.findAllByType("fa");

        assertEquals(2, found.size());
    }
}
