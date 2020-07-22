package ru.citeck.ecos.uiserv.domain.journal.api.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.citeck.ecos.uiserv.domain.journal.service.JournalConfigService;

import java.util.Locale;

@RestController
@RequestMapping("/api/journalcfg")
@Transactional
public class JournalConfigApi {
    @Autowired
    private JournalConfigService journalConfigService;

    @GetMapping
    public JournalConfigService.JournalConfigDownstream getJournalConfig(@RequestParam String journalId, Locale locale) {
        return journalConfigService.getJournalConfig(journalId).get();
    }
}
