package ru.citeck.ecos.uiserv.domain.menu.service.resolving.resolvers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records3.RecordsService;
import ru.citeck.ecos.uiserv.domain.menu.service.resolving.ResolvedMenuItemDto;
import ru.citeck.ecos.uiserv.domain.journal.service.JournalService;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class DirectJournalsResolver implements MenuItemsResolver {

    private final JournalsResolver journalsResolver;
    private static final String ID = "JOURNALS";
    private static final String LIST_ID_KEY = "listId";

    @Autowired
    public DirectJournalsResolver(RecordsService recordsService, JournalService journalService) {
        journalsResolver = new JournalsResolver(recordsService, this::getIdFromParams, journalService);
    }

    private JournalsResolver.IDs getIdFromParams(Map<String, String> params, ResolvedMenuItemDto context) {
        final String id = JournalsResolver.getParam(params, context, LIST_ID_KEY);
        return new JournalsResolver.IDs(id, id);
    }

    @Override
    public List<ResolvedMenuItemDto> resolve(Map<String, String> params, ResolvedMenuItemDto context, Function<String, String> i18n) {
        return journalsResolver.resolve(params, context);
    }

    @Override
    public String getId() {
        return ID;
    }
}
