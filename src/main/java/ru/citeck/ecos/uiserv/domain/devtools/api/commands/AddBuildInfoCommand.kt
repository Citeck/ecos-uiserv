package ru.citeck.ecos.uiserv.domain.devtools.api.commands

import org.springframework.stereotype.Component
import ru.citeck.ecos.commands.CommandExecutor
import ru.citeck.ecos.commands.annotation.CommandType
import ru.citeck.ecos.uiserv.domain.devtools.api.records.BuildInfoRecords
import ru.citeck.ecos.uiserv.domain.devtools.dto.AppBuildInfo

@Component
class AddBuildInfoCommandExecutor(
    private val records: BuildInfoRecords
) : CommandExecutor<AddBuildInfoCommand> {

    override fun execute(command: AddBuildInfoCommand): Any? {
        command.info.forEach { records.registerBuildInfo(it) }
        return true
    }
}

@CommandType("uiserv.add-build-info")
class AddBuildInfoCommand(
    val info: List<AppBuildInfo>
)
