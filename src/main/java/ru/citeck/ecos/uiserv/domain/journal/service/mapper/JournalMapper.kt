package ru.citeck.ecos.uiserv.domain.journal.service.mapper

import lombok.extern.slf4j.Slf4j
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json.mapper
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.uiserv.domain.journal.dto.*
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalEntity
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalRepository
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
@Slf4j
class JournalMapper(
    private val repository: JournalRepository
) {

    fun entityToDto(entity: JournalEntity): JournalWithMeta {

        val dto = JournalWithMeta(true)

        dto.journalDef = JournalDef.create()
            .withId(entity.extId)
            .withEditable(entity.editable)
            .withSourceId(entity.sourceId)
            .withMetaRecord(EntityRef.valueOf(entity.metaRecord))
            .withName(mapper.read(entity.name, MLText::class.java))
            .withTypeRef(EntityRef.valueOf(entity.typeRef))
            .withPredicate(mapper.read(entity.predicate, Predicate::class.java))
            .withDefaultFilters(mapper.readList(entity.defaultFilters, Predicate::class.java))
            .withQueryData(mapper.read(entity.queryData, ObjectData::class.java))
            .withSearchConfig(mapper.read(entity.searchConfig, JournalSearchConfig::class.java))
            .withActionsFromType(entity.actionsFromType)
            .withActions(mapper.readList(entity.actions, EntityRef::class.java))
            .withActionsDef(mapper.readList(entity.actionsDef, JournalActionDef::class.java))
            .withProperties(mapper.read(entity.attributes, ObjectData::class.java))
            .withGroupBy(mapper.readList(entity.groupBy, String::class.java))
            .withDefaultSortBy(mapper.readList(entity.sortBy, JournalSortByDef::class.java))
            .withComputed(mapper.readList(entity.computed, JournalComputedDef::class.java))
            .withColumns(mapper.readList(entity.columns, JournalColumnDef::class.java))
            .withSystem(entity.system)
            .build()

        dto.modified = entity.lastModifiedDate
        dto.modifier = entity.lastModifiedBy
        dto.created = entity.createdDate
        dto.creator = entity.createdBy

        return dto
    }

    fun dtoToEntity(journal: JournalDef): JournalEntity {

        var entity = repository.findByExtId(journal.id).orElse(null)
        if (entity == null) {
            entity = JournalEntity()
            entity.extId = journal.id
        }

        entity.sourceId = journal.sourceId
        entity.metaRecord = journal.metaRecord.toString()
        entity.columns = mapper.toString(journal.columns.map { mapper.toNonDefaultJson(it) })!!
        entity.editable = journal.editable
        entity.name = mapper.toString(journal.name)
        entity.typeRef = EntityRef.toString(journal.typeRef)
        entity.predicate = mapper.toString(journal.predicate)
        entity.defaultFilters = mapper.toString(journal.defaultFilters)
        entity.queryData = mapper.toString(journal.queryData)
        entity.searchConfig = mapper.toString(journal.searchConfig)
        entity.actionsFromType = journal.actionsFromType
        entity.actions = mapper.toString(journal.actions)
        entity.actionsDef = mapper.toString(journal.actionsDef)
        entity.attributes = mapper.toString(journal.properties)
        entity.groupBy = mapper.toString(journal.groupBy)
        entity.sortBy = mapper.toString(journal.defaultSortBy)
        entity.computed = mapper.toString(journal.computed)
        entity.system = journal.system

        return entity
    }
}
