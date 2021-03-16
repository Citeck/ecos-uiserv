package ru.citeck.ecos.uiserv.domain.menu.service.testutils

import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsProperties
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.uiserv.app.common.service.AuthoritiesSupport
import ru.citeck.ecos.uiserv.domain.ecostype.dto.EcosTypeInfo
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService
import ru.citeck.ecos.uiserv.domain.i18n.service.MessageResolver
import ru.citeck.ecos.uiserv.domain.menu.api.records.MenuRecords
import ru.citeck.ecos.uiserv.domain.menu.api.records.ResolvedMenuRecords
import ru.citeck.ecos.uiserv.domain.menu.dao.MenuDao
import ru.citeck.ecos.uiserv.domain.menu.service.MenuService
import ru.citeck.ecos.uiserv.domain.menu.service.format.MenuReaderService
import java.util.concurrent.ConcurrentHashMap

open class MenuTestBase {

    protected lateinit var menuDao: MenuDao
    protected lateinit var menuService: MenuService
    protected lateinit var resolvedMenuRecords: ResolvedMenuRecords
    protected lateinit var records: RecordsService

    private val typesInfo = ConcurrentHashMap<String, EcosTypeInfo>()

    @BeforeEach
    fun before() {

        menuDao = MenuInMemDao()
        val menuReaderService = MenuReaderService()

        val recordsServices = object : RecordsServiceFactory() {
            override fun createProperties(): RecordsProperties {
                val props = super.createProperties()
                props.appName = "uiserv"
                props.appInstanceId = "uiserv-1234567"
                return props
            }
        }

        records = recordsServices.recordsServiceV1

        menuService = MenuService(menuDao, menuReaderService, AuthoritiesSupport(), recordsServices.recordsService)

        val menuRecords = MenuRecords(menuService, object: MessageResolver {
            override fun getMessage(key: String): String {
                return key
            }
        })
        records.register(menuRecords)

        val ecosTypeService = Mockito.mock(EcosTypeService::class.java)
        Mockito.`when`(ecosTypeService.getTypeInfo(Mockito.any(RecordRef::class.java))).thenAnswer {
            val typeRef: RecordRef = it.getArgument(0)
            typesInfo[typeRef.id]
        }

        resolvedMenuRecords = ResolvedMenuRecords(menuRecords, ecosTypeService, AuthoritiesSupport())
        records.register(resolvedMenuRecords)

        typesInfo.clear()
    }

    fun registerType(data: Any) {

        val type = Json.mapper.convert(data, EcosTypeInfo::class.java)
        val id = type?.id
        if (id.isNullOrBlank()) {
            error("Incorrect type without id: $data")
        }

        typesInfo[id] = type
    }
}

