package ru.citeck.ecos.uiserv.domain.form.service

import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.uiserv.domain.form.repo.EcosFormEntity

interface FormsEntityDao {

    fun count(): Long

    fun count(predicate: Predicate): Long

    fun findByExtId(formId: String, workspace: String): EcosFormEntity?

    fun findAllByExtIdIn(ids: Set<String>, workspace: String): Set<EcosFormEntity>

    fun save(entity: EcosFormEntity): EcosFormEntity

    fun findAll(
        predicate: Predicate,
        max: Int,
        skip: Int,
        sort: List<SortBy>
    ): List<EcosFormEntity>

    fun findFirstByFormKey(formKey: String): EcosFormEntity?

    fun delete(entity: EcosFormEntity)

    fun findAllByTypeRef(typeRef: String): List<EcosFormEntity>

    fun findAllByTypeRefIn(typeRefs: List<String>): List<EcosFormEntity>
}
