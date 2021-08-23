package ru.citeck.ecos.uiserv.domain.form

import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.uiserv.domain.form.repo.EcosFormEntity
import ru.citeck.ecos.uiserv.domain.form.service.FormsEntityDao

class FormsEntityInMemDao(
    private val predicateService: PredicateService
) : FormsEntityDao {

    private val entities: MutableMap<String, EcosFormEntity> = HashMap()

    override fun count(): Long {
        return entities.size.toLong()
    }

    override fun findByExtId(formId: String): EcosFormEntity? {
        return entities[formId]
    }

    override fun save(entity: EcosFormEntity): EcosFormEntity {
        entities[entity.extId] = entity
        return entity
    }

    override fun findAll(predicate: Predicate, max: Int, skip: Int): List<EcosFormEntity> {
        if (max == 0) {
            return emptyList()
        }
        val mappedPred = PredicateUtils.mapValuePredicates(predicate) {
            if (it.getAttribute() == "moduleId") {
                ValuePredicate("extId", it.getType(), it.getValue())
            } else {
                it
            }
        }
        val fixedMax = if (max < 0) {
            1000
        } else {
            max
        }
        return predicateService.filter(entities.values, mappedPred).drop(skip).take(fixedMax)
    }

    override fun findFirstByFormKey(formKey: String): EcosFormEntity? {
        return entities.values.firstOrNull { it.formKey == formKey }
    }

    override fun delete(entity: EcosFormEntity) {
        entities.remove(entity.extId)
    }

    override fun findAllByTypeRef(typeRef: String): List<EcosFormEntity> {
        return entities.values.filter { it.typeRef == typeRef }
    }

    override fun findAllByTypeRefIn(typeRefs: List<String>): List<EcosFormEntity> {
        return entities.values.filter { it.typeRef != null && typeRefs.contains(it.typeRef) }
    }
}
