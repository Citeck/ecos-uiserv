package ru.citeck.ecos.uiserv.domain.journal.service.settings

import org.apache.commons.lang.StringUtils
import org.springframework.stereotype.Service
import ru.citeck.ecos.uiserv.app.common.service.AuthoritiesSupport
import ru.citeck.ecos.uiserv.app.security.constants.AuthoritiesConstants
import ru.citeck.ecos.uiserv.app.security.service.SecurityUtils
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalSettingsDto
import ru.citeck.ecos.uiserv.domain.journal.repo.JournalSettingsEntity

@Service
class JournalSettingsPermissionsServiceImpl(
        private val authoritiesSupport: AuthoritiesSupport
) : JournalSettingsPermissionsService {

    @Override
    override fun canRead(entity: JournalSettingsEntity): Boolean {
        if (isAdmin()) {
            return true
        }

        if (getCurrentUsername() == entity.authority) {
            return true
        }

        val isAuthority = authoritiesSupport.currentUserAuthorities.stream()
                .anyMatch { it == entity.authority }
        if (isAuthority) {
            return true
        }

        return getCurrentUsername() == entity.createdBy
    }

    @Override
    override fun canRead(dto: JournalSettingsDto): Boolean {
        if (isAdmin()) {
            return true
        }

        if (getCurrentUsername() == dto.authority) {
            return true
        }

        val isAuthority = authoritiesSupport.currentUserAuthorities.stream()
                .anyMatch { it == dto.authority }
        if (isAuthority) {
            return true
        }

        return getCurrentUsername() == dto.creator
    }

    @Override
    override fun canWrite(entity: JournalSettingsEntity): Boolean {
        if (isAdmin()) {
            return true
        }

        return getCurrentUsername() == entity.createdBy
    }

    @Override
    override fun canWrite(dto: JournalSettingsDto): Boolean {
        if (isAdmin()) {
            return true
        }

        return getCurrentUsername() == dto.creator
    }

    @Override
    override fun canWriteNew(dto: JournalSettingsDto): Boolean {
        if (isAdmin()) {
            return true
        }

        return getCurrentUsername() == dto.authority
    }

    private fun getCurrentUsername(): String {
        var username = SecurityUtils.getCurrentUserLoginFromRequestContext()
        require(!StringUtils.isBlank(username)) { "Username cannot be empty" }
        if (username.contains("people@")) {
            username = username.replaceFirst("people@".toRegex(), "")
        }
        if (username.contains("alfresco/")) {
            username = username.replaceFirst("alfresco/".toRegex(), "")
        }
        return username
    }

    private fun isAdmin(): Boolean {
        return authoritiesSupport.currentUserAuthorities.stream()
                .anyMatch { it == AuthoritiesConstants.ADMIN }
    }

}