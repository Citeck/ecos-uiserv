package ru.citeck.ecos.uiserv.service.menu.format.json;

import org.springframework.stereotype.Component;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.uiserv.service.menu.dto.MenuDto;
import ru.citeck.ecos.uiserv.service.menu.format.MenuReader;

import java.util.Collections;
import java.util.List;

@Component
public class JsonMenuReader implements MenuReader {

    @Override
    public MenuDto readData(byte[] data) {
        return Json.getMapper().read(data, MenuDto.class);
    }

    @Override
    public List<String> getExtensions() {
        return Collections.singletonList("json");
    }
}
