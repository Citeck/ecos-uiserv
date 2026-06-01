package ru.citeck.ecos.uiserv.domain.board.cardorder.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDtoDao
import ru.citeck.ecos.uiserv.domain.board.cardorder.dto.MoveCardConfig
import ru.citeck.ecos.uiserv.domain.board.cardorder.service.BoardCardOrderService

/**
 * Action-dispatch records source for board operations. Mutate with
 * `{ "action": "<name>", "config": { ... } }`; the config shape depends on the action.
 */
@Component
class BoardsServiceRecordsDao(
    private val service: BoardCardOrderService
) : AbstractRecordsDao(),
    RecordMutateDtoDao<BoardsServiceRecordsDao.Command> {

    companion object {
        const val ID = "boards-service"
        const val ACTION_MOVE_CARD = "move-card"
    }

    override fun getId() = ID

    override fun getRecToMutate(recordId: String): Command = Command()

    override fun saveMutatedRec(record: Command): String {
        val action = record.action
        require(!action.isNullOrBlank()) { "'action' is required" }
        val config = record.config ?: DataValue.createObj()
        when (action) {
            ACTION_MOVE_CARD -> {
                val cfg = config.getAs(MoveCardConfig::class.java)
                    ?: error("Invalid config for action '$action'")
                service.moveCard(cfg)
            }
            else -> error("Unknown action: '$action'")
        }
        return action
    }

    class Command {
        var action: String? = null
        var config: DataValue? = null
    }
}
