package ru.citeck.ecos.uiserv.service.menu.resolving.resolvers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.uiserv.journal.service.type.TypeJournalService;
import ru.citeck.ecos.uiserv.web.rest.menu.dto.Element;
import ru.citeck.ecos.uiserv.service.menu.resolving.ResolvedMenuItemDto;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class DirectJournalsResolver implements MenuItemsResolver {
    private final JournalsResolver journalsResolver;
    private static final String ID = "JOURNALS";
    private static final String LIST_ID_KEY = "listId";

    @Autowired
    public DirectJournalsResolver(RecordsService recordsService, TypeJournalService typeJournalService) {
        journalsResolver = new JournalsResolver(recordsService, this::getIdFromParams, typeJournalService);
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
