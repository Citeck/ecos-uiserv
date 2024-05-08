package ru.citeck.ecos.uiserv.domain.journalsettings.service

import org.springframework.stereotype.Service
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.uiserv.domain.journalsettings.dto.JournalSettingsDto
import ru.citeck.ecos.uiserv.domain.journalsettings.repo.JournalSettingsEntity

@Service
class JournalSettingsPermissionsServiceImpl : JournalSettingsPermissionsService {

    @Override
    override fun canRead(entity: JournalSettingsEntity): Boolean {
        val currentUser = AuthContext.getCurrentUser()

        if (AuthContext.isRunAsAdmin() || AuthContext.isRunAsSystem()) {
            return true
        }
        if (currentUser == entity.authority) {
            return true
        }
        if (entity.authorities?.contains(currentUser) == true) {
            return true
        }
        val isAuthority = AuthContext.getCurrentUserWithAuthorities().stream()
            .anyMatch { it == entity.authority || entity.authorities?.contains(it) == true }
        if (isAuthority) {
            return true
        }
        return currentUser == entity.createdBy
    }

    @Override
    override fun canRead(dto: JournalSettingsDto): Boolean {
        val currentUser = AuthContext.getCurrentUser()

        if (AuthContext.isRunAsAdmin() || AuthContext.isRunAsSystem()) {
            return true
        }
        if (dto.authorities.contains(currentUser)) {
            return true
        }
        val isAuthority = AuthContext.getCurrentUserWithAuthorities().stream()
            .anyMatch { dto.authorities.contains(it) }
        if (isAuthority) {
            return true
        }
        return currentUser == dto.creator
    }

    @Override
    override fun canWrite(entity: JournalSettingsEntity): Boolean {
        if (AuthContext.isRunAsAdmin() || AuthContext.isRunAsSystem()) {
            return true
        }
        return AuthContext.getCurrentUser() == entity.createdBy
    }

    @Override
    override fun canWrite(dto: JournalSettingsDto): Boolean {
        if (AuthContext.isRunAsAdmin() || AuthContext.isRunAsSystem()) {
            return true
        }
        return AuthContext.getCurrentUser() == dto.creator
    }

    @Override
    override fun canWriteNew(dto: JournalSettingsDto): Boolean {
        if (AuthContext.isRunAsAdmin() || AuthContext.isRunAsSystem()) {
            return true
        }
        val currentUser = AuthContext.getCurrentUser()
        return dto.authorities.contains(currentUser)
    }
}
