package ru.citeck.ecos.uiserv.service.dashdoard;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.apps.app.module.EappsModuleService;
import ru.citeck.ecos.apps.app.module.type.ui.dashboard.DashboardModule;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.objdata.ObjectData;
import ru.citeck.ecos.records2.utils.json.JsonUtils;
import ru.citeck.ecos.uiserv.domain.DashboardDto;
import ru.citeck.ecos.uiserv.domain.DashboardEntity;
import ru.citeck.ecos.uiserv.repository.DashboardRepository;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository repo;
    private final EappsModuleService eappsModuleService;
    private final RecordsService recordsService;
    private TaskScheduler taskScheduler;

    private String dashboardTypeId;
    private Map<String, PublishWaitingInfo> publishWaitingMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        dashboardTypeId = eappsModuleService.getTypeId(DashboardModule.class);
        if (taskScheduler != null) {
            taskScheduler.scheduleWithFixedDelay(this::cleanUpPublishWaiting, Duration.ofMinutes(1));
        }
    }

    private void cleanUpPublishWaiting() {

        long currentTime = System.currentTimeMillis();

        publishWaitingMap.keySet().forEach(k -> {
            PublishWaitingInfo info = publishWaitingMap.get(k);
            if (currentTime - info.getCreatedTime() > 300_000) {
                publishWaitingMap.remove(k);
            }
        });
    }

    public List<DashboardDto> getAllDashboards() {
        return repo.findAll()
            .stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    public Optional<DashboardDto> getDashboardById(String id) {
        return repo.findByExtId(id).map(this::mapToDto);
    }

    public Optional<DashboardDto> getDashboard(String type, String key, String user) {
        return getDashboardEntity(type, key, user).map(this::mapToDto);
    }

    public Optional<DashboardDto> getFirstDashboardByKeys(String type, List<String> key, String user) {
        return key.stream()
            .map(k -> getDashboardEntity(type, k, user))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(this::mapToDto)
            .findFirst();
    }

    public DashboardDto saveDashboard(DashboardDto dashboard) {

        DashboardEntity entity = mapToEntity(dashboard);
        DashboardDto dashboardDto = mapToDto(repo.save(entity));

        PublishWaitingInfo info = publishWaitingMap.remove(dashboardDto.getId());
        if (info != null) {
            info.future.complete(dashboardDto);
        }

        return dashboardDto;
    }

    public Future<DashboardDto> saveDashboardWithEapps(DashboardDto dashboard) {

        DashboardDto toSave = new DashboardDto(dashboard);

        Optional<DashboardDto> optDashboardDto = getDashboard(
            toSave.getType(),
            toSave.getKey(),
            toSave.getAuthority()
        );

        if (optDashboardDto.isPresent()) {

            DashboardDto newDto = optDashboardDto.get();
            newDto.setConfig(toSave.getConfig());
            toSave = newDto;

        } else {

            toSave.setId(UUID.randomUUID().toString());
        }

        ObjectData dashboardModuleData = JsonUtils.convert(toSave, ObjectData.class);
        dashboardModuleData.set("module_id", toSave.getId());

        RecordRef moduleRecRef = RecordRef.create("eapps", "module", dashboardTypeId + "$");
        RecordMeta meta = new RecordMeta(moduleRecRef, dashboardModuleData);

        PublishWaitingInfo info = new PublishWaitingInfo();
        publishWaitingMap.put(toSave.getId(), info);
        recordsService.mutate(meta);

        return info.future;
    }

    public void removeDashboard(String id) {
        repo.findByExtId(id).ifPresent(repo::delete);
    }

    private Optional<DashboardEntity> getDashboardEntity(String type, String key, String user) {

        if (StringUtils.isBlank(key)) {
            key = "DEFAULT";
        }

        Optional<DashboardEntity> entity;

        if (StringUtils.isNotBlank(user)) {
            entity = repo.findForAuthority(type, key, user);
        } else {
            entity = repo.findForAll(type, key);
        }

        return entity;
    }

    private DashboardDto mapToDto(DashboardEntity entity) {

        DashboardDto dto = new DashboardDto();

        dto.setId(entity.getExtId());
        dto.setAuthority(entity.getAuthority());
        dto.setConfig(JsonUtils.read(entity.getConfig(), ObjectData.class));
        dto.setKey(entity.getKey());
        dto.setType(entity.getType());

        return dto;
    }

    private DashboardEntity mapToEntity(DashboardDto dto) {

        DashboardEntity entity = getDashboardEntity(dto.getType(), dto.getKey(), dto.getAuthority())
            .orElseGet(DashboardEntity::new);

        if (dto.getConfig() != null && dto.getConfig().size() > 0) {
            entity.setConfig(JsonUtils.toBytes(dto.getConfig()));
        }

        entity.setExtId(dto.getId());
        entity.setAuthority(dto.getAuthority());
        entity.setType(dto.getType());

        if (StringUtils.isBlank(dto.getKey())) {
            entity.setKey("DEFAULT");
        } else {
            entity.setKey(dto.getKey());
        }

        return entity;
    }

    @Autowired(required = false)
    public void setTaskScheduler(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    @Data
    private static class PublishWaitingInfo {

        private CompletableFuture<DashboardDto> future = new CompletableFuture<>();
        private long createdTime = System.currentTimeMillis();
    }
}
