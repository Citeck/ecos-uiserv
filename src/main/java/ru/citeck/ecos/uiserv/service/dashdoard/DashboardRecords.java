package ru.citeck.ecos.uiserv.service.dashdoard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.app.module.EappsModuleService;
import ru.citeck.ecos.apps.app.module.type.ui.dashboard.DashboardModule;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.uiserv.domain.DashboardDto;
import ru.citeck.ecos.uiserv.service.entity.AbstractEntityRecords;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author Roman Makarskiy
 */
@Slf4j
@Component
public class DashboardRecords extends AbstractEntityRecords<DashboardDto> {

    public static final String ID = "dashboard";

    private static final long PUBLISH_TIMEOUT_MS = 10_000;

    private final RecordsService recordsService;

    private final String dashboardTypeId;
    private final Map<String, CompletableFuture<String>> publishWaitingMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public DashboardRecords(DashboardEntityService entityService,
                            EappsModuleService eappsModuleService,
                            RecordsService recordsService) {
        setId(ID);
        this.entityService = entityService;
        this.recordsService = recordsService;
        dashboardTypeId = eappsModuleService.getTypeId(DashboardModule.class);
    }

    @Override
    public RecordsMutResult save(List<DashboardDto> values) {

        List<String> records = values.stream()
            .map(this::save)
            .collect(Collectors.toList());

        long waitingTime = System.currentTimeMillis() + PUBLISH_TIMEOUT_MS;
        for (String record : records) {
            try {
                Future<String> future = publishWaitingMap.get(record);
                if (future != null) {
                    future.get(PUBLISH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                }
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.error("Publish waiting failed. Values: " + values, e);
            }
            if (System.currentTimeMillis() > waitingTime) {
                log.error("Publish waiting failed by timeout. "
                    + "Waiting timeout = " + waitingTime
                    + " actual time: " + System.currentTimeMillis()
                    + " Values: " + values);
            }
        }

        RecordsMutResult recordsMutResult = new RecordsMutResult();
        recordsMutResult.setRecords(records.stream()
            .map(RecordMeta::new)
            .collect(Collectors.toList()));
        return recordsMutResult;
    }

    private String save(DashboardDto dto) {

        DashboardDto toSave = new DashboardDto(dto);

        Optional<DashboardDto> optDashboardDto = entityService.getByKey(
            toSave.getType(),
            toSave.getKey(),
            toSave.getUser()
        );
        if (optDashboardDto.isPresent()) {

            DashboardDto newDto = optDashboardDto.get();
            newDto.setConfig(toSave.getConfig());
            toSave = newDto;

        } else if (!StringUtils.isBlank(toSave.getId())) {
            optDashboardDto = entityService.getById(toSave.getId());
            if (optDashboardDto.isPresent()) {
                toSave.setId("");
            }
        }

        if (StringUtils.isBlank(toSave.getId())) {
            toSave.setId(UUID.randomUUID().toString());
        }

        ObjectNode dashboardModuleData = objectMapper.valueToTree(toSave);
        dashboardModuleData.set("module_id", TextNode.valueOf(toSave.getId()));

        String recordLocalId = dashboardTypeId + "$";
        RecordMeta meta = new RecordMeta(RecordRef.create("eapps", "module", recordLocalId));
        meta.setAttributes(dashboardModuleData);

        publishWaitingMap.put(toSave.getId(), new CompletableFuture<>());
        recordsService.mutate(meta);

        return toSave.getId();
    }

    public void wasPublished(DashboardDto dto) {
        CompletableFuture<String> future = publishWaitingMap.get(dto.getId());
        if (future != null) {
            future.complete(dto.getId());
            publishWaitingMap.remove(dto.getId());
        }
    }

    @Override
    protected DashboardDto getEmpty() {
        return new DashboardDto();
    }
}
