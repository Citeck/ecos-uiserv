package ru.citeck.ecos.uiserv.service.menu.format;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.uiserv.service.menu.dto.MenuDto;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MenuReaderService {

    private final Map<String, MenuReader> readers = new ConcurrentHashMap<>();

    public MenuDto readMenu(byte[] data, String filename) {
        String extension = FilenameUtils.getExtension(filename);
        return readers.get(extension).readData(data);
    }

    @Autowired
    public void setReaders(List<MenuReader> readers) {
        readers.forEach(reader ->
            reader.getExtensions().forEach(ext ->
                this.readers.put(ext, reader)
            )
        );
    }
}
