package ru.citeck.ecos.uiserv.domain.menu.service.testutils

import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import org.springframework.util.ResourceUtils
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.io.file.EcosFile
import ru.citeck.ecos.commons.io.file.std.EcosStdFile
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.config.lib.provider.InMemConfigProvider
import ru.citeck.ecos.config.lib.records.CfgRecordsDao
import ru.citeck.ecos.config.lib.service.EcosConfigServiceFactory
import ru.citeck.ecos.model.lib.ModelServiceFactory
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.test.commons.EcosWebAppApiMock
import ru.citeck.ecos.uiserv.Application
import ru.citeck.ecos.uiserv.domain.ecostype.service.EcosTypeService
import ru.citeck.ecos.uiserv.domain.i18n.service.MessageResolver
import ru.citeck.ecos.uiserv.domain.menu.api.records.MenuRecords
import ru.citeck.ecos.uiserv.domain.menu.api.records.ResolvedMenuRecords
import ru.citeck.ecos.uiserv.domain.menu.dao.MenuDao
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDeployArtifact
import ru.citeck.ecos.uiserv.domain.menu.dto.MenuDto
import ru.citeck.ecos.uiserv.domain.menu.dto.SubMenuDef
import ru.citeck.ecos.uiserv.domain.menu.eapps.MenuArtifactHandler
import ru.citeck.ecos.uiserv.domain.menu.service.MenuService
import ru.citeck.ecos.uiserv.domain.menu.service.format.MenuReaderService
import ru.citeck.ecos.uiserv.domain.menu.service.format.json.JsonMenuReader
import ru.citeck.ecos.webapp.api.EcosWebAppApi
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import java.io.File
import java.util.concurrent.ConcurrentHashMap

open class MenuTestBase {

    protected lateinit var menuDao: MenuDao
    protected lateinit var menuService: MenuService
    protected lateinit var resolvedMenuRecords: ResolvedMenuRecords
    protected lateinit var records: RecordsService

    protected lateinit var menuArtifactHandler: MenuArtifactHandler

    private val typesInfo = ConcurrentHashMap<String, TypeDef>()

    lateinit var configs: InMemConfigProvider

    @BeforeEach
    fun before() {

        menuDao = MenuInMemDao()
        val menuReaderService = MenuReaderService()
        menuReaderService.setReaders(listOf(JsonMenuReader()))

        val webAppContext = EcosWebAppApiMock(Application.NAME)

        val recordsServices = object : RecordsServiceFactory() {
            override fun getEcosWebAppApi(): EcosWebAppApi {
                return webAppContext
            }
        }

        records = recordsServices.recordsService
        val configServiceFactory = EcosConfigServiceFactory()
        configs = configServiceFactory.inMemConfigProvider
        records.register(CfgRecordsDao(configServiceFactory))

        val modelServices = ModelServiceFactory()

        menuService = MenuService(
            menuDao,
            menuReaderService,
            modelServices.workspaceService,
            recordsServices.recordsService
        )

        val menuRecords = MenuRecords(
            menuService,
            object : MessageResolver {
                override fun getMessage(key: String): String {
                    return key
                }
            }
        )
        records.register(menuRecords)

        val ecosTypeService = Mockito.mock(EcosTypeService::class.java)

        Mockito.`when`(ecosTypeService.getTypeInfo(Mockito.any(EntityRef::class.java))).thenAnswer {
            val typeRef: EntityRef = it.getArgument(0)
            typesInfo[typeRef.getLocalId()]
        }
        Mockito.`when`(ecosTypeService.getTypeRefByJournal(Mockito.any())).thenAnswer { invocation ->
            typesInfo.values.firstOrNull {
                it.journalRef == invocation.getArgument(0)
            }?.id?.let { ModelUtils.getTypeRef(it) }
        }

        resolvedMenuRecords = ResolvedMenuRecords(menuRecords, ecosTypeService)
        records.register(resolvedMenuRecords)

        menuArtifactHandler = MenuArtifactHandler(menuService)

        typesInfo.clear()
    }

    fun deployMenuArtifacts() {
        val configs = File("./src/main/resources/eapps/artifacts/ui/menu").listFiles() ?: emptyArray()
        for (file in configs) {
            if (!file.name.endsWith(".json")) {
                continue
            }
            val artifact = MenuDeployArtifact()
            artifact.filename = file.name
            artifact.data = file.readBytes()
            artifact.id = Json.mapper.readDataNotNull(artifact.data)["id"].asText()
            menuArtifactHandler.deployArtifact(artifact)
        }
    }

    fun registerType(data: Any) {

        val type = Json.mapper.convert(data, TypeDef::class.java)
        val id = type?.id
        if (id.isNullOrBlank()) {
            error("Incorrect type without id: $data")
        }

        typesInfo[id] = type
    }

    fun loadType(path: String): TypeDef {
        val file = ResourceUtils.getFile("classpath:test/menu/" + this::class.simpleName + "/type/" + path)
        return Json.mapper.read(file, TypeDef::class.java)!!
    }

    fun loadMenu(path: String): MenuDto {
        val file = ResourceUtils.getFile("classpath:test/menu/" + this::class.simpleName + "/menu/" + path)
        return Json.mapper.read(file, MenuDto::class.java)!!
    }

    fun loadSubMenu(path: String): SubMenuDef {
        val file = ResourceUtils.getFile("classpath:test/menu/" + this::class.simpleName + "/subMenu/" + path)
        return Json.mapper.read(file, SubMenuDef::class.java)!!
    }

    fun loadJson(path: String): DataValue {
        val file = ResourceUtils.getFile("classpath:test/menu/" + this::class.simpleName + "/" + path)
        return Json.mapper.read(file, DataValue::class.java)!!
    }

    fun loadAndRegisterAllTypes(path: String) {

        val file = ResourceUtils.getFile("classpath:test/menu/" + this::class.simpleName + "/" + path)
        val ecosFile = EcosStdFile(file)

        ecosFile.findFiles("**.{json,yml,yaml}").forEach {
            registerType(Json.mapper.read(it, DataValue::class.java)!!)
        }
    }

    fun findFiles(pattern: String): List<EcosFile> {

        val file = ResourceUtils.getFile("classpath:test/menu/" + this::class.simpleName)
        val ecosFile = EcosStdFile(file)

        return ecosFile.findFiles(pattern)
    }
}
