package ru.citeck.ecos.uiserv.domain.form.service

import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.uiserv.domain.form.repo.EcosFormEntity

interface FormsEntityDao {

    fun count(): Long

    fun findByExtId(formId: String): EcosFormEntity?

    fun save(entity: EcosFormEntity): EcosFormEntity

    fun findAll(predicate: Predicate, max: Int, skip: Int): List<EcosFormEntity>

    fun findFirstByFormKey(formKey: String): EcosFormEntity?

    fun delete(entity: EcosFormEntity)

    fun findAllByTypeRef(typeRef: String): List<EcosFormEntity>

    fun findAllByTypeRefIn(typeRefs: List<String>): List<EcosFormEntity>
}
