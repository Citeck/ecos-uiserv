package ru.citeck.ecos.uiserv.app.common.utils

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.AttributePredicate
import ru.citeck.ecos.records2.predicate.model.EmptyPredicate
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.webapp.api.entity.EntityRef

object WorkspaceRefPredicateUtils {

    const val WORKSPACE_REF_ATT = "workspaceRef"
    const val WORKSPACE_ATT = "workspace"

    /**
     * Menus and dashboards retain admin$* workspaces; other stores normalize them to the global ID.
     */
    fun interface GlobalWorkspaceRule {
        fun isGlobal(workspaceId: String): Boolean
    }

    @JvmStatic
    fun globalWsRuleOf(workspaceService: WorkspaceService): GlobalWorkspaceRule {
        return GlobalWorkspaceRule { workspaceService.isWorkspaceWithGlobalEntities(it) }
    }

    @JvmStatic
    fun blankOrDefaultWsRule(): GlobalWorkspaceRule {
        return GlobalWorkspaceRule { it.isBlank() || it == ModelUtils.DEFAULT_WORKSPACE_ID }
    }

    /**
     * Global aliases map to the empty storage ID. Empty composite predicates must retain their
     * semantics rather than collapse to null and become alwaysFalse.
     */
    @JvmStatic
    @JvmOverloads
    fun mapWorkspaceRefPredicate(
        predicate: Predicate?,
        globalWorkspaceRule: GlobalWorkspaceRule,
        attsMapping: Map<String, String> = emptyMap()
    ): Predicate {
        return PredicateUtils.mapAttributePredicates(
            predicate,
            { pred: AttributePredicate ->
                when {
                    pred.getAttribute() == WORKSPACE_REF_ATT -> {
                        if (pred is ValuePredicate && isWorkspaceRefSelection(pred)) {
                            val ids = replaceWorkspaceRefsToIds(pred.getValue(), globalWorkspaceRule)
                            if (pred.getValue().isArray()) {
                                Predicates.inVals(WORKSPACE_ATT, DataValue.of(ids).toStrList())
                            } else {
                                Predicates.eq(WORKSPACE_ATT, ids)
                            }
                        } else if (pred is ValuePredicate) {
                            val copy = pred.copy<ValuePredicate>()
                            copy.setAtt(WORKSPACE_ATT)
                            // Remaining CONTAINS/LIKE values are free text, not record refs.
                            if (isRefValuedType(pred)) {
                                copy.setVal(replaceWorkspaceRefsToIds(pred.getValue(), globalWorkspaceRule))
                            }
                            copy
                        } else {
                            val copy = pred.copy<AttributePredicate>()
                            copy.setAtt(WORKSPACE_ATT)
                            copy
                        }
                    }
                    attsMapping.containsKey(pred.getAttribute()) -> {
                        val copy = pred.copy<AttributePredicate>()
                        copy.setAtt(attsMapping[pred.getAttribute()])
                        copy
                    }
                    else -> pred
                }
            },
            onlyAnd = false,
            optimize = false,
            filterEmptyComposite = false
        ) ?: Predicates.alwaysFalse()
    }

    /**
     * Text search must not widen workspace scope. Explicit selections also count inside OR/NOT,
     * otherwise scoping would silently exclude matches from those branches.
     */
    @JvmStatic
    fun isWorkspaceScopingSkipAllowed(mappedPredicate: Predicate?): Boolean {
        if (!AuthContext.isRunAsSystemOrAdmin()) {
            return false
        }
        var explicitWorkspaceFilter = false
        PredicateUtils.mapAttributePredicates(
            mappedPredicate,
            { pred: AttributePredicate ->
                if (pred.getAttribute() == WORKSPACE_ATT && isExplicitWorkspaceFilter(pred)) {
                    explicitWorkspaceFilter = true
                }
                pred
            },
            onlyAnd = false,
            optimize = false,
            filterEmptyComposite = false
        )
        return explicitWorkspaceFilter
    }

    /** The association picker emits CONTAINS with refs, while global search sends free text. */
    private fun isWorkspaceRefSelection(pred: ValuePredicate): Boolean {
        if (pred.getType() != ValuePredicate.Type.CONTAINS) {
            return false
        }
        val value = pred.getValue()
        val values = if (value.isArray()) value.toStrList() else listOf(value.asText())
        return values.isNotEmpty() &&
            values.all {
                val ref = EntityRef.valueOf(it)
                ref.getAppName() == "emodel" && ref.getSourceId() == "workspace" && ref.getLocalId().isNotBlank()
            }
    }

    private fun isExplicitWorkspaceFilter(pred: AttributePredicate): Boolean {
        return pred is EmptyPredicate || (pred is ValuePredicate && isRefValuedType(pred))
    }

    private fun isRefValuedType(pred: ValuePredicate): Boolean {
        return pred.getType() == ValuePredicate.Type.EQ || pred.getType() == ValuePredicate.Type.IN
    }

    private fun replaceWorkspaceRefsToIds(value: Any?, rule: GlobalWorkspaceRule): Any? {
        if (value == null) {
            return null
        }
        val dataValue = DataValue.of(value)
        return if (dataValue.isArray()) {
            val result = DataValue.createArr()
            for (element in dataValue) {
                result.add(toStoredWorkspaceId(element.asText(), rule))
            }
            result
        } else {
            DataValue.createAsIs(toStoredWorkspaceId(dataValue.asText(), rule))
        }
    }

    private fun toStoredWorkspaceId(ref: String, rule: GlobalWorkspaceRule): String {
        val localId = EntityRef.valueOf(ref).getLocalId()
        return if (rule.isGlobal(localId)) {
            ""
        } else {
            localId
        }
    }
}
