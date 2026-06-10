package ru.citeck.ecos.uiserv.domain.board.cardorder.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDtoDao
import ru.citeck.ecos.uiserv.domain.board.cardorder.dto.MoveCardAction
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
                val cfg = config.getAs(MoveCardAction::class.java)
                    ?: error("Invalid config for action '$action'")
                // Workspace travels inside `config` (config.workspace): the sibling `_workspace`
                // control-att isn't delivered to a custom mutate DTO. The value is client-supplied but
                // NOT a trusted-and-unchecked scope: board-card-order is workspaceScope=PRIVATE, so
                // ecos-data authorizes it — create enforces the caller's workspace membership (a workspace
                // the user can't access fails the create and rolls back the move), and reads filter by the
                // user's available workspaces. So it can't grant cross-workspace access.
                service.moveCard(cfg, cfg.workspace)
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
