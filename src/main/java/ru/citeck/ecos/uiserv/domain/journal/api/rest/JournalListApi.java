package ru.citeck.ecos.uiserv.domain.journal.api.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.citeck.ecos.uiserv.domain.journal.service.JournalListService;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/journallist")
@Transactional
public class JournalListApi {
    @Autowired
    private JournalListService journalListService;

    @GetMapping
    public JournalListService.JournalListDownstream getJournalConfig(@RequestParam String listId, Locale locale) {
        return journalListService.get(listId, locale).get();
    }

    @GetMapping("/list")
    public List<JournalListService.JournalListDownstream> listConfigs(Locale locale) {
        return journalListService.list(locale);
    }
}