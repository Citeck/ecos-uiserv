package ru.citeck.ecos.uiserv.domain.form

import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService
import ru.citeck.ecos.uiserv.domain.form.api.records.EcosFormRecordsDao
import ru.citeck.ecos.uiserv.domain.form.api.records.EcosResolvedFormRecordsDao
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormService
import ru.citeck.ecos.uiserv.domain.form.service.EcosFormServiceImpl
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef

abstract class FormsTestBase {

    lateinit var recordsService: RecordsService
    lateinit var ecosFormService: EcosFormService

    var types: MutableMap<RecordRef, TypeDef> = HashMap()
    var typeByForm: MutableMap<RecordRef, RecordRef> = HashMap()

    @BeforeEach
    fun before() {
        val services = RecordsServiceFactory()
        recordsService = services.recordsServiceV1
        ecosFormService = EcosFormServiceImpl(FormsEntityInMemDao(services.predicateService), services.recordsService)

        val formsRecordsDao = EcosFormRecordsDao(ecosFormService, null)
        recordsService.register(formsRecordsDao)

        val typeService = Mockito.mock(EcosTypeService::class.java)
        Mockito.`when`(typeService.getTypeInfo(Mockito.any())).thenAnswer {
            types[it.getArgument(0)]
        }
        Mockito.`when`(typeService.getTypeRefByForm(Mockito.any())).thenAnswer {
            typeByForm[it.getArgument(0)]
        }

        val resolvedFormsRecordsDao = EcosResolvedFormRecordsDao(formsRecordsDao, typeService, ecosFormService)
        recordsService.register(resolvedFormsRecordsDao)
    }
}
