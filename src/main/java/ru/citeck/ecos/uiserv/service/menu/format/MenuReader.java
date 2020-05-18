package ru.citeck.ecos.uiserv.service.menu.format;

import ru.citeck.ecos.uiserv.service.menu.dto.MenuDto;

import java.util.List;

public interface MenuReader {

    MenuDto readData(byte[] data);

    List<String> getExtensions();
}
