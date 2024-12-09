package ru.citeck.ecos.uiserv.domain.evaluator.service

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.citeck.ecos.uiserv.domain.evaluator.RecordEvaluator
import ru.citeck.ecos.uiserv.domain.evaluator.RecordEvaluatorService

@Component
class RecordEvaluatorsRegistrar(
    private val recordEvaluatorService: RecordEvaluatorService
) {

    companion object {
        val log = KotlinLogging.logger {}
    }

    private var evaluators: List<RecordEvaluator<*, *, *>>? = null

    @PostConstruct
    fun register() {
        log.info { "========================== RecordEvaluatorsRegistrar ==========================" }
        if (evaluators != null) {
            evaluators!!.forEach { this.register(it) }
        }
        log.info { "========================= /RecordEvaluatorsRegistrar ==========================" }
    }

    private fun register(evaluator: RecordEvaluator<*, *, *>) {
        log.info { "Register: \"" + evaluator.type + "\" with class " + evaluator.javaClass.name }
        recordEvaluatorService.register(evaluator)
    }

    @Autowired(required = false)
    fun setEvaluators(evaluators: List<RecordEvaluator<*, *, *>>) {
        this.evaluators = evaluators
    }
}
