package ru.citeck.ecos.uiserv.domain.action.impl

import org.springframework.stereotype.Component
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.emitter.EmitterConfig
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao
import ru.citeck.ecos.uiserv.domain.action.dto.ActionDto
import ru.citeck.ecos.uiserv.domain.action.service.ActionService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.toEntityRef

@Component
class UserEventActionRecords(
    private val eventsService: EventsService,
    private val actionService: ActionService
) : AbstractRecordsDao(), RecordMutateDao {

    companion object {
        const val ID = "user-event"

        private const val RECORD_ATT = "record"
        private const val EVENT_DATA_ATT = "eventData"
        private const val PERMISSIONS_READ_ATT = "permissions._has.Read"

        private const val ENTITY_REF_POSTFIX = "?id}"
    }

    override fun getId(): String {
        return ID
    }

    override fun mutate(record: LocalRecordAtts): String {
        require(record.id.isNotBlank()) { "Record id is required" }
        val userEventType = record.id

        val eventRecord = record.attributes[RECORD_ATT].toEntityRef()
        check(allowFireEventForRecord(eventRecord)) {
            "Fire event for record is not allowed. You should have read permissions for record"
        }

        val actionDefinition = actionService.getAction(userEventType) ?: error("Action not found: $userEventType")
        val actionEventDataFirstLevelStringAtts = getEventDataFirstLevelStringAtts(actionDefinition)

        fun shouldBeConvertedToEntityRef(attribute: String): Boolean {
            return actionEventDataFirstLevelStringAtts[attribute]?.endsWith(ENTITY_REF_POSTFIX) == true
        }

        val eventData = mutableMapOf<String, Any>()
        record.attributes[EVENT_DATA_ATT].forEach { key, value ->
            if (shouldBeConvertedToEntityRef(key)) {
                eventData[key] = value.toEntityRef()
            } else {
                eventData[key] = value
            }
        }

        if (eventRecord.isNotEmpty()) {
            // Record att must be a EntityRef explicitly
            eventData[RECORD_ATT] = eventRecord
        }

        val emitter = eventsService.getEmitter(
            EmitterConfig.create<Any> {
                eventType = userEventType
                eventClass = Any::class.java
            }
        )

        emitter.emit(eventData)

        return "ok"
    }

    private fun getEventDataFirstLevelStringAtts(action: ActionDto): Map<String, String> {
        return action.config[EVENT_DATA_ATT].asMap(String::class.java, Any::class.java).map {
            if (it.value is String) {
                it.key to it.value as String
            } else {
                null
            }
        }.filterNotNull().toMap()
    }


    private fun allowFireEventForRecord(record: EntityRef): Boolean {
        return record.isEmpty() || recordsService.getAtt(record, PERMISSIONS_READ_ATT).asBoolean()
    }
}
