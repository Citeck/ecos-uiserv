package ru.citeck.ecos.uiserv.service.journal.link;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.uiserv.domain.JournalFilter;
import ru.citeck.ecos.uiserv.repository.JournalFilterRepository;

import java.util.UUID;

@Service
@Transactional
public class JournalFiltersService {
    private static final int LIMIT = 1000;

    private JournalFilterRepository journalFilterRepository;

    public JournalFiltersService(JournalFilterRepository journalFilterRepository) {
        this.journalFilterRepository = journalFilterRepository;
    }

    public JournalFilter save(JournalFilter journalFilter) {
        journalFilter.setExternalId(UUID.randomUUID().toString());
        JournalFilter result = journalFilterRepository.save(journalFilter);

        removeLatestIfExceedsLimit(journalFilter.getUserName());

        return result;
    }

    private void removeLatestIfExceedsLimit(String userName) {
        int exceededCount = journalFilterRepository.countByUserName(userName) - LIMIT;

        for (int i = 0; i < exceededCount; i++) {
            removeLatest(userName);
        }
    }

    private void removeLatest(String userName) {
        JournalFilter latest = journalFilterRepository.findTopByUserNameOrderByCreationTimeAsc(userName);
        journalFilterRepository.delete(latest);
    }

    @Nullable
    public JournalFilter findByExternalId(String externalId) {
        return journalFilterRepository.findByExternalId(externalId);
    }

    public void deleteByExternalId(String externalId) {
        journalFilterRepository.deleteByExternalId(externalId);
    }
}
