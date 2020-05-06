package ru.citeck.ecos.uiserv.web.rest.menu.resolvers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.uiserv.journal.service.type.TypeJournalService;
import ru.citeck.ecos.uiserv.web.rest.menu.dto.Element;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class SiteJournalsResolver implements MenuItemsResolver {

    private final JournalsResolver journalsResolver;

    private static final String ID = "SITE_JOURNALS";
    private static final String JOURNAL_NAME_TEMPLATE = "site-%s-%s"; /* site-<sitename>-<listId> */
    private static final String SITE_ID_KEY = "siteId";
    private static final String LIST_ID_KEY = "listId";
    private static final String DEFAULT_JOURNAL_LIST_ID = "main";

    @Autowired
    public SiteJournalsResolver(RecordsService recordsService, TypeJournalService typeJournalService) {
        journalsResolver = new JournalsResolver(recordsService, this::getIdFromParams, typeJournalService);
    }

    private JournalsResolver.IDs getIdFromParams(Map<String, String> params, Element context) {
        String siteId = JournalsResolver.getParam(params, context, SITE_ID_KEY);
        String listId = JournalsResolver.getParam(params, context, LIST_ID_KEY);

        if (listId == null || listId.equals("")) {
            listId = DEFAULT_JOURNAL_LIST_ID;
        }

        return new JournalsResolver.IDs(
            String.format(JOURNAL_NAME_TEMPLATE, siteId, listId),
            listId);
    }

    @Override
    public List<Element> resolve(Map<String, String> params, Element context,
                                 Function<String, String> i18n) {
        return journalsResolver.resolve(params, context);
    }

    @Override
    public String getId() {
        return ID;
    }
}
