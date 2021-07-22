package ru.citeck.ecos.uiserv.domain.admin.eapps

import org.springframework.stereotype.Component
import ru.citeck.ecos.apps.app.domain.handler.EcosArtifactHandler
import ru.citeck.ecos.uiserv.domain.admin.dto.AdminSectionsGroupDef
import ru.citeck.ecos.uiserv.domain.admin.service.AdminSectionsGroupService
import java.util.function.Consumer

@Component
class AdminSectionsGroupArtifactHandler(
    val adminSectionsGroupService: AdminSectionsGroupService
) : EcosArtifactHandler<AdminSectionsGroupDef> {

    override fun deployArtifact(artifact: AdminSectionsGroupDef) {
        adminSectionsGroupService.save(artifact)
    }

    override fun deleteArtifact(artifactId: String) {
        error("Unsupported")
    }

    override fun getArtifactType(): String {
        return "ui/admin-sections-group"
    }

    override fun listenChanges(listener: Consumer<AdminSectionsGroupDef>) {
    }
}
