package ru.citeck.ecos.uiserv.web.rest.menu.resolvers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.uiserv.web.rest.menu.dto.Element;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class DirectJournalsResolver implements MenuItemsResolver {
    private final JournalsResolver journalsResolver;
    private static final String ID = "JOURNALS";
    private static final String LIST_ID_KEY = "listId";

    @Autowired
    public DirectJournalsResolver(RecordsService recordsService) {
        journalsResolver = new JournalsResolver(recordsService, this::getIdFromParams);
    }

    private JournalsResolver.IDs getIdFromParams(Map<String, String> params, Element context) {
        final String id = JournalsResolver.getParam(params, context, LIST_ID_KEY);
        return new JournalsResolver.IDs(id, id);
    }

    @Override
    public List<Element> resolve(Map<String, String> params, Element context, Function<String, String> i18n) {
        return journalsResolver.resolve(params, context);
    }

    @Override
    public String getId() {
        return ID;
    }
}
