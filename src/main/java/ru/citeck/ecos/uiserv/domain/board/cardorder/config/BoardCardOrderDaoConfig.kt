package ru.citeck.ecos.uiserv.domain.board.cardorder.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.data.sql.domain.DbDomainConfig
import ru.citeck.ecos.data.sql.domain.DbDomainFactory
import ru.citeck.ecos.data.sql.records.DbRecordsDaoConfig
import ru.citeck.ecos.data.sql.records.perms.DbPermsComponent
import ru.citeck.ecos.data.sql.records.perms.DbRecordPerms
import ru.citeck.ecos.data.sql.service.DbDataServiceConfig
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.uiserv.domain.board.cardorder.BoardCardOrderDesc

@Configuration
class BoardCardOrderDaoConfig {

    @Bean
    fun boardCardOrderDao(dbDomainFactory: DbDomainFactory): RecordsDao = dbDomainFactory.create(
        DbDomainConfig.create()
            .withRecordsDao(
                DbRecordsDaoConfig.create {
                    withId(BoardCardOrderDesc.SOURCE_ID)
                    withTypeRef(ModelUtils.getTypeRef(BoardCardOrderDesc.TYPE_ID))
                }
            )
            .withDataService(
                DbDataServiceConfig.create {
                    withTable(BoardCardOrderDesc.DB_TABLE)
                    withStoreTableMeta(true)
                }
            ).build()
    ).withSchema("ecos_data")
        .withPermsComponent(AllReadPermsComponent())
        .build()

    /**
     * v1: ordering records are visible/writable to anyone (the public write path goes through a
     * separate service that enforces board/card permissions). No business logic should mutate
     * board-card-order records directly via the records API.
     */
    private class AllReadPermsComponent : DbPermsComponent {
        override fun getRecordPerms(user: String, authorities: Set<String>, record: Any): DbRecordPerms {
            return object : DbRecordPerms {
                override fun getAdditionalPerms(): Set<String> = emptySet()
                override fun getAuthoritiesWithReadPermission(): Set<String> = setOf("EVERYONE")
                override fun hasAttReadPerms(name: String): Boolean = true
                override fun hasAttWritePerms(name: String): Boolean = true
                override fun hasReadPerms(): Boolean = true
                override fun hasWritePerms(): Boolean = true
            }
        }
    }
}
