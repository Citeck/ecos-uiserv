package ru.citeck.ecos.uiserv.domain.journal.dto

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.json.serialization.annotation.IncludeNonDefault
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.uiserv.domain.action.dto.ActionConfirmDef
import ru.citeck.ecos.uiserv.domain.action.dto.ExecForQueryConfig

@IncludeNonDefault
@JsonDeserialize(builder = JournalActionDef.Builder::class)
data class JournalActionDef(

    val id: String,
    val name: MLText,
    val pluralName: MLText,

    var preActionModule: String,

    val icon: String,
    val confirm: ActionConfirmDef,

    val type: String,
    val config: ObjectData,

    val features: Map<String, Boolean>,

    val execForRecordsBatchSize: Int,
    val execForRecordsParallelBatchesCount: Int,

    val execForQueryConfig: ExecForQueryConfig,

    val timeoutErrorMessage: MLText,

    val predicate: Predicate
) {

    companion object {

        val EMPTY = create {}

        @JvmStatic
        fun create(): Builder {
            return Builder()
        }

        @JvmStatic
        fun create(builder: Builder.() -> Unit): JournalActionDef {
            val builderObj = Builder()
            builder.invoke(builderObj)
            return builderObj.build()
        }
    }

    fun copy(): Builder {
        return Builder(this)
    }

    fun copy(builder: Builder.() -> Unit): JournalActionDef {
        val builderObj = Builder(this)
        builder.invoke(builderObj)
        return builderObj.build()
    }

    open class Builder() {

        var id: String = ""
        var name: MLText = MLText.EMPTY
        var pluralName: MLText = MLText.EMPTY
        var preActionModule: String = ""
        var icon: String = ""
        var confirm: ActionConfirmDef = ActionConfirmDef.EMPTY
        var type: String = ""
        var config: ObjectData = ObjectData.create()
        var features: Map<String, Boolean> = emptyMap()
        var execForRecordsBatchSize: Int = 0
        var execForRecordsParallelBatchesCount: Int = 1
        var execForQueryConfig: ExecForQueryConfig = ExecForQueryConfig.EMPTY
        var timeoutErrorMessage: MLText = MLText.EMPTY
        var predicate: Predicate = VoidPredicate.INSTANCE

        constructor(base: JournalActionDef) : this() {
            this.id = base.id
            this.name = base.name
            this.pluralName = base.pluralName
            this.preActionModule = base.preActionModule
            this.icon = base.icon
            this.confirm = base.confirm
            this.type = base.type
            this.config = ObjectData.deepCopyOrNew(base.config)
            this.features = base.features
            this.execForRecordsBatchSize = base.execForRecordsBatchSize
            this.execForRecordsParallelBatchesCount = base.execForRecordsParallelBatchesCount
            this.execForQueryConfig = base.execForQueryConfig
            this.timeoutErrorMessage = base.timeoutErrorMessage
            this.predicate = Json.mapper.copy(base.predicate) ?: VoidPredicate.INSTANCE
        }

        fun withConfigMap(map: Map<String, String>) {
            val obj = ObjectData.create()
            map.forEach { (k, v) -> obj.set(k, v) }
            withConfig(obj)
        }

        fun withId(id: String): Builder {
            this.id = id
            return this
        }

        fun withName(name: MLText?): Builder {
            this.name = name ?: MLText.EMPTY
            return this
        }

        fun withPluralName(pluralName: MLText?): Builder {
            this.pluralName = pluralName ?: MLText.EMPTY
            return this
        }

        fun withPreActionModule(preActionModule: String?): Builder {
            this.preActionModule = preActionModule ?: ""
            return this
        }

        fun withIcon(icon: String?): Builder {
            this.icon = icon ?: ""
            return this
        }

        fun withConfirm(confirm: ActionConfirmDef?): Builder {
            this.confirm = confirm ?: ActionConfirmDef.EMPTY
            return this
        }

        fun withType(type: String?): Builder {
            this.type = type ?: ""
            return this
        }

        fun withConfig(config: ObjectData?): Builder {
            this.config = config ?: ObjectData.create()
            return this
        }

        fun withFeatures(features: Map<String, Boolean?>?): Builder {
            this.features = if (features == null) {
                emptyMap()
            } else {
                val newFeatures = hashMapOf<String, Boolean>()
                features.forEach { (k, v) ->
                    if (v != null) {
                        newFeatures[k] = v
                    }
                }
                newFeatures
            }
            return this
        }

        fun withExecForRecordsBatchSize(execForRecordsBatchSize: Int?): Builder {
            this.execForRecordsBatchSize = execForRecordsBatchSize ?: 0
            return this
        }

        fun withExecForRecordsParallelBatchesCount(execForRecordsParallelBatchesCount: Int?): Builder {
            this.execForRecordsParallelBatchesCount = execForRecordsParallelBatchesCount ?: 1
            return this
        }

        fun withExecForQueryConfig(execForQueryConfig: ExecForQueryConfig?): Builder {
            this.execForQueryConfig = execForQueryConfig ?: ExecForQueryConfig.EMPTY
            return this
        }

        fun withTimeoutErrorMessage(timeoutErrorMessage: MLText?): Builder {
            this.timeoutErrorMessage = timeoutErrorMessage ?: MLText.EMPTY
            return this
        }

        fun withPredicate(predicate: Predicate?): Builder {
            this.predicate = predicate ?: VoidPredicate.INSTANCE
            return this
        }

        fun build(): JournalActionDef {
            return JournalActionDef(
                id,
                name,
                pluralName,
                preActionModule,
                icon,
                confirm,
                type,
                config,
                features,
                execForRecordsBatchSize,
                execForRecordsParallelBatchesCount,
                execForQueryConfig,
                timeoutErrorMessage,
                predicate
            )
        }
    }
}
