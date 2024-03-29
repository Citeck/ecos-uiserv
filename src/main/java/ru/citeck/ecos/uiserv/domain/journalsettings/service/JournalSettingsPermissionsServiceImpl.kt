package ru.citeck.ecos.uiserv.domain.journalsettings.service

import org.springframework.stereotype.Service
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.uiserv.domain.journalsettings.dto.JournalSettingsDto
import ru.citeck.ecos.uiserv.domain.journalsettings.repo.JournalSettingsEntity

@Service
class JournalSettingsPermissionsServiceImpl : JournalSettingsPermissionsService {

    @Override
    override fun canRead(entity: JournalSettingsEntity): Boolean {

        if (AuthContext.isRunAsAdmin() || AuthContext.isRunAsSystem()) {
            return true
        }
        if (AuthContext.getCurrentUser() == entity.authority) {
            return true
        }
        val isAuthority = AuthContext.getCurrentUserWithAuthorities().stream()
            .anyMatch { it == entity.authority }
        if (isAuthority) {
            return true
        }
        return AuthContext.getCurrentUser() == entity.createdBy
    }

    @Override
    override fun canRead(dto: JournalSettingsDto): Boolean {

        if (AuthContext.isRunAsAdmin() || AuthContext.isRunAsSystem()) {
            return true
        }
        if (AuthContext.getCurrentUser() == dto.authority) {
            return true
        }
        val isAuthority = AuthContext.getCurrentUserWithAuthorities().stream()
            .anyMatch { it == dto.authority }
        if (isAuthority) {
            return true
        }
        return AuthContext.getCurrentUser() == dto.creator
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
        return AuthContext.getCurrentUser() == dto.authority
    }
}
