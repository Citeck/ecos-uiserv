package ru.citeck.ecos.uiserv.domain.admin.api.records.eapps;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.app.domain.handler.EcosArtifactHandler;
import ru.citeck.ecos.uiserv.domain.admin.api.records.dto.AdminSecGroupDto;
import ru.citeck.ecos.uiserv.domain.admin.api.records.service.AdminSecGroupService;

import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class AdminSecGroupArtifactHandler implements EcosArtifactHandler<AdminSecGroupDto> {

    private final AdminSecGroupService adminSecGroupService;

    @Override
    public void deployArtifact(@NotNull AdminSecGroupDto dto) {
        adminSecGroupService.save(dto);
    }

    @NotNull
    @Override
    public String getArtifactType() {
        return "ui/admin-sections-group";
    }

    @Override
    public void listenChanges(@NotNull Consumer<AdminSecGroupDto> consumer) {

    }
}
