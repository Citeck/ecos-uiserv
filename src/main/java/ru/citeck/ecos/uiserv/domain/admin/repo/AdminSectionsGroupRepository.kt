package ru.citeck.ecos.uiserv.domain.admin.repo

import org.springframework.data.jpa.repository.JpaRepository

interface AdminSectionsGroupRepository : JpaRepository<AdminSectionsGroupEntity, Long> {

    fun findByExtId(extId: String): AdminSectionsGroupEntity?
}
