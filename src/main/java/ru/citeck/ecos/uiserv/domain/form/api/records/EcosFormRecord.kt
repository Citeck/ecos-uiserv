package ru.citeck.ecos.uiserv.domain.form.api.records

import com.fasterxml.jackson.annotation.JsonValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.YamlUtils.toNonDefaultString
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthContext.isRunAsAdmin
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.nio.charset.StandardCharsets

class EcosFormRecord(
    private val appName: String,
    private val sourceId: String,

    @AttName("...")
    val def: EcosFormDef,
    private val workspaceService: WorkspaceService
) {

    @AttName(ScalarType.ID_SCHEMA)
    fun getRef(): EntityRef {
        val formId = def.id
        val localId = if (formId.startsWith("type$")) {
            formId
        } else {
            workspaceService.addWsPrefixToId(formId, def.workspace)
        }
        return EntityRef.create(appName, sourceId, localId)
    }

    fun getModuleId(): String {
        return def.id
    }

    fun getDisplayName(): MLText {
        val title: MLText = def.title
        return if (MLText.isEmpty(title)) {
            MLText("Form")
        } else {
            title
        }
    }

    fun getWorkspaceRef(): EntityRef {
        return def.workspace.let {
            if (it.isBlank()) {
                EntityRef.EMPTY
            } else {
                EntityRef.create(AppName.EMODEL, "workspace", it)
            }
        }
    }

    @JsonValue
    fun toJson(): EcosFormDef {
        return def.copy {
            withWorkspace("")

            var typeLocalId = def.typeRef.getLocalId()
            if (typeLocalId.isNotBlank()) {
                typeLocalId = workspaceService.replaceWsPrefixFromIdToMask(typeLocalId)
                withTypeRef(def.typeRef.withLocalId(typeLocalId))
            }
        }
    }

    fun getData(): ByteArray {
        return toNonDefaultString(toJson()).toByteArray(StandardCharsets.UTF_8)
    }

    fun getEcosType(): String {
        return "form"
    }

    fun getPermissions(): Permissions {
        return Permissions()
    }

    inner class Permissions : AttValue {

        override fun has(name: String): Boolean {
            return if (name.equals("write", ignoreCase = true)) {
                val formId: String = this@EcosFormRecord.def.id
                if (formId.contains("$")) {
                    false
                } else {
                    isRunAsAdmin() && !EcosFormRecordsDao.SYSTEM_FORMS.contains(formId)
                    if (EcosFormRecordsDao.SYSTEM_FORMS.contains(formId)) {
                        false
                    } else {
                        val workspace = this@EcosFormRecord.def.workspace
                        isRunAsAdmin() || workspaceService.isUserManagerOf(AuthContext.getCurrentUser(), workspace)
                    }
                }
            } else {
                name.equals("read", ignoreCase = true)
            }
        }
    }
}
