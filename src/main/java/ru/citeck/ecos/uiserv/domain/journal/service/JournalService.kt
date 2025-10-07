package ru.citeck.ecos.uiserv.domain.journal.service

import ru.citeck.ecos.model.lib.workspace.IdInWs
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalDef
import ru.citeck.ecos.uiserv.domain.journal.dto.JournalWithMeta
import ru.citeck.ecos.uiserv.domain.journal.service.provider.JournalsProvider
import java.util.function.BiConsumer
import java.util.function.Consumer

interface JournalService {

    fun getLastModifiedTimeMs(): Long

    fun getJournalById(id: IdInWs): JournalWithMeta?

    fun getAll(predicate: Predicate, max: Int, skip: Int, sort: List<SortBy>): List<JournalWithMeta>

    fun getAll(maxItems: Int, skipCount: Int): Set<JournalWithMeta>

    fun getAll(extIds: Collection<IdInWs>): List<JournalWithMeta>

    fun getCount(): Long

    fun getCount(predicate: Predicate): Long

    fun onJournalDeleted(consumer: Consumer<JournalWithMeta>)

    fun onJournalChanged(consumer: BiConsumer<JournalDef?, JournalDef?>)

    fun onJournalWithMetaChanged(consumer: BiConsumer<JournalWithMeta?, JournalWithMeta?>)

    fun save(dto: JournalDef): JournalWithMeta

    fun delete(id: IdInWs)

    fun registerProvider(provider: JournalsProvider)
}
