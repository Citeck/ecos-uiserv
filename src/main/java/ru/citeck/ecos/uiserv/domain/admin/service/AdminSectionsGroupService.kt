package ru.citeck.ecos.uiserv.domain.admin.service

import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json.mapper
import ru.citeck.ecos.uiserv.domain.admin.dto.AdminSectionDef
import ru.citeck.ecos.uiserv.domain.admin.dto.AdminSectionsGroupDef
import ru.citeck.ecos.uiserv.domain.admin.dto.AdminSectionsGroupDef.Companion.create
import ru.citeck.ecos.uiserv.domain.admin.repo.AdminSectionsGroupEntity
import ru.citeck.ecos.uiserv.domain.admin.repo.AdminSectionsGroupRepository

@Service
@RequiredArgsConstructor
class AdminSectionsGroupService(
    val adminSectionsGroupRepository: AdminSectionsGroupRepository
) {

    @Transactional
    fun save(dto: AdminSectionsGroupDef): AdminSectionsGroupDef {
        return mapToDto(adminSectionsGroupRepository.save(mapToEntity(dto)))
    }

    fun findAll(): List<AdminSectionsGroupDef> {
        return adminSectionsGroupRepository.findAll()
            .map { entity: AdminSectionsGroupEntity -> mapToDto(entity) }
            .sortedBy { it.order }
    }

    private fun mapToDto(entity: AdminSectionsGroupEntity): AdminSectionsGroupDef {
        return create()
            .withId(entity.extId)
            .withName(mapper.read(entity.name, MLText::class.java))
            .withOrder(entity.groupOrder)
            .withSections(DataValue.create(entity.sections).asList(AdminSectionDef::class.java))
            .build()
    }

    private fun mapToEntity(dto: AdminSectionsGroupDef): AdminSectionsGroupEntity {
        var entity = adminSectionsGroupRepository.findByExtId(dto.id)
        if (entity == null) {
            entity = AdminSectionsGroupEntity()
            entity.extId = dto.id
        }
        entity.groupOrder = dto.order
        entity.name = mapper.toString(dto.name)
        entity.sections = mapper.toString(dto.sections) ?: "[]"
        return entity
    }
}
