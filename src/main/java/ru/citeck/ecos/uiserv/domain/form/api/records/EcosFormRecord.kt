package ru.citeck.ecos.uiserv.domain.form.api.records

import ecos.com.fasterxml.jackson210.annotation.JsonValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.YamlUtils.toNonDefaultString
import ru.citeck.ecos.context.lib.auth.AuthContext.isRunAsAdmin
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.uiserv.domain.form.dto.EcosFormDef
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.nio.charset.StandardCharsets

class EcosFormRecord(
    private val appName: String,
    private val sourceId: String,

    @AttName("...")
    val def: EcosFormDef
) {

    @AttName("?id")
    fun getId(): EntityRef {
        return EntityRef.create(appName, sourceId, def.id)
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

    @JsonValue
    @com.fasterxml.jackson.annotation.JsonValue
    fun toJson(): EcosFormDef {
        return def
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
                }
            } else {
                name.equals("read", ignoreCase = true)
            }
        }
    }
}
