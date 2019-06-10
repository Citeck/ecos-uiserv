package ru.citeck.ecos.uiserv.service.dashdoard;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.uiserv.domain.DashboardDTO;
import ru.citeck.ecos.uiserv.domain.File;
import ru.citeck.ecos.uiserv.domain.FileType;
import ru.citeck.ecos.uiserv.service.file.FileService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * @author Roman Makarskiy
 */
@Component
@RequiredArgsConstructor
public class DashboardRepositoryImpl implements DashboardRepository {

    private final FileService fileService;
    private final ObjectMapper objectMapper;

    @Override
    public DashboardDTO create(DashboardDTO dashboardDTO) {
        DashboardDTO result = new DashboardDTO();

        final String id = UUID.randomUUID().toString();
        result.setId(id);

        result.setKey(dashboardDTO.getKey());
        result.setConfig(dashboardDTO.getConfig());

        save(result);
        return result;
    }

    @Override
    public DashboardDTO read(String id) {
        return fileService.loadFile(FileType.DASHBOARD, id)
            .map(this::fromJson)
            .orElse(null);
    }

    private DashboardDTO fromJson(File file) {
        try {
            return objectMapper.readValue(file.getFileVersion().getBytes(), DashboardDTO.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed convert File to DashboardDTO", e);
        }
    }

    @Override
    public DashboardDTO update(DashboardDTO dashboardDTO) {
        DashboardDTO result = new DashboardDTO();
        result.setId(dashboardDTO.getId());
        result.setKey(dashboardDTO.getKey());
        result.setConfig(dashboardDTO.getConfig());

        save(result);
        return result;
    }

    private void save(DashboardDTO dto) {
        fileService.deployFileOverride(FileType.DASHBOARD, dto.getId(), null,
            toJson(dto), Collections.singletonMap("key", dto.getKey()));
    }

    private byte[] toJson(DashboardDTO dashboardDTO) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            objectMapper.writeValue(output, dashboardDTO);
            return output.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed convert DashboardDTO to json", e);
        }
    }

    @Override
    public void delete(String id) {
        fileService.deployFileOverride(FileType.DASHBOARD, id, null, null, null);
    }

    @Override
    public DashboardDTO getByKey(String key) {
        List<File> found = fileService.find("key", Collections.singletonList(key));
        if (found.isEmpty()) {
            return null;
        }
        if (found.size() > 1) {
            throw new RuntimeException("More than one dashboard found by key: " + key);
        }
        return fromJson(found.get(0));
    }

}
