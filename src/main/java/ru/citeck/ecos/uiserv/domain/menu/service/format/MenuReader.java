package ru.citeck.ecos.uiserv.domain.menu.service.format;

import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDto;

import java.util.List;

public interface MenuReader {

    MenuDto readData(byte[] data);

    List<String> getExtensions();
}
