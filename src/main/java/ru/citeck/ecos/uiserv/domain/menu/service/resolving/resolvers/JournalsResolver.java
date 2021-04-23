package ru.citeck.ecos.uiserv.domain.menu.service.resolving.resolvers;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName;
import ru.citeck.ecos.records2.request.query.QueryConsistency;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.uiserv.domain.menu.service.resolving.ResolvedMenuItemDto;
import ru.citeck.ecos.uiserv.domain.journal.service.JournalService;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class JournalsResolver {

    private static final String LIST_ID_KEY = "listId";
    private static final String JOURNAL_REF_KEY = "journalRef";
    private static final String JOURNAL_LINK_KEY = "JOURNAL_LINK";
    private static final String JOURNAL_ID_KEY = "journalId";

    private final RecordsService recordsService;
    private final JournalListIdExtractor journalListIdExtractor;
    private final JournalService journalService;

    public interface JournalListIdExtractor {
        IDs extract(Map<String, String> params, ResolvedMenuItemDto context);
    }

    public List<ResolvedMenuItemDto> resolve(Map<String, String> params, ResolvedMenuItemDto context) {
        final IDs listId = journalListIdExtractor.extract(params, context);
        return queryJournalsRefs(listId.getId()).stream()
            .map(journalInfo -> constructItem(journalInfo, params, context, listId.getId(), listId.getSimpleId()))
            .collect(Collectors.toList());
    }

    /**
     * First trying to get parameter by @key from @context, then from resolver @params
     * @param params resolver params
     * @param context element context
     * @param key parameter key
     * @return parameter or empty String
     */
    public static String getParam(Map<String, String> params, ResolvedMenuItemDto context, String key) {
        String result = null;
        if (context != null) {
            Map<String, String> contextParams = context.getParams();
            if (MapUtils.isNotEmpty(contextParams)) {
                result = contextParams.get(key);
            }
        }
        if (StringUtils.isEmpty(result) && MapUtils.isNotEmpty(params)) {
            result = params.get(key);
        }
        return StringUtils.defaultString(result);
    }

    private static String toUpperCase(String s) {
        if (StringUtils.isEmpty(s)) {
            return "";
        }
        String result = s.replaceAll("\\W", "_");
        return result.toUpperCase();
    }


    protected static class JournalListAtts {
        @AttName("journal:journals")
        private List<JournalAtts> journals;

        public List<JournalAtts> getJournals() {
            return journals;
        }

        public void setJournals(List<JournalAtts> journals) {
            this.journals = journals;
        }
    }

    private List<JournalAtts> queryJournalsRefs(String journalList) {
        if (journalList == null || journalList.equals(""))
            return Collections.emptyList();

        RecordsQuery query = new RecordsQuery();
        query.setSourceId("alfresco/");
        query.setQuery(String.format("TYPE:\"journal:journalsList\" AND =cm:name:\"%s\"",
            journalList));
        query.setLanguage("fts-alfresco");
        query.setConsistency(QueryConsistency.TRANSACTIONAL);

        RecordsQueryResult<JournalListAtts> result = recordsService.queryRecords(query, JournalListAtts.class);
        List<JournalAtts> journals = result.getRecords()
            .stream()
            .flatMap(journalListAtts -> journalListAtts.getJournals().stream())
            .collect(Collectors.toList());

/*        journalService.getJournalsByJournalList(journalList).forEach(journal -> {
            JournalAtts atts = new JournalAtts();
            atts.setId(journal.getId());
            atts.setJournalType(journal.getId());
            atts.setName(MLText.getClosestValue(journal.getLabel(), LocaleContextHolder.getLocale()));
            atts.setTitle(atts.getName());
            journals.add(atts);
        });*/

        return journals;
    }

    protected ResolvedMenuItemDto constructItem(JournalAtts journalInfo, Map<String, String> params, ResolvedMenuItemDto context,
                                                String journalListId, String journalListSimpleId) {
        /* get data */
        String title = journalInfo.getTitle();
        String journalId = journalInfo.getJournalType();//RepoUtils.getProperty(journalRef, JournalsModel.PROP_JOURNAL_TYPE , nodeService);
        String elemIdVar = toUpperCase(journalId);
        String parentElemId = StringUtils.defaultString(context.getId());
        String elemId = String.format("%s_%s_JOURNAL", parentElemId, elemIdVar);
        Boolean displayCount = Boolean.parseBoolean(getParam(params, context, "displayCount"));
        String countForJournalsParam = getParam(params, context, "countForJournals");
        Set<String> countForJournals;
        if (displayCount && StringUtils.isNotEmpty(countForJournalsParam)) {
            countForJournals = new HashSet<>(Arrays.asList(countForJournalsParam.split(",")));
            displayCount = countForJournals.contains(journalId);
        }
        Boolean displayIcon = context.getParams().containsKey("rootElement");

        /* icon. if journal element is placed in root category */
        String icon = null;
        if (displayIcon) {
            icon = journalId;
        }

        /* put all action params from parent (siteName or listId) */
        Map<String, String> actionParams = new HashMap<>();
        if (context.getAction() != null) {
            Map<String, String> parentActionParams = context.getAction().getParams();
            actionParams.putAll(parentActionParams);
        }
        if (journalListId != null) {
            actionParams.put(LIST_ID_KEY, journalListSimpleId);
        }
        /* current element action params */
        actionParams.put(JOURNAL_REF_KEY, journalInfo.getId());

        /* current element params */
        Map<String, String> elementParams = new HashMap<>();

        //todo restore counters
//        /* badge (items count) */
//        if (displayCount) {
//            RequestKey requestKey = new RequestKey(journalId);
//            Long count = itemsCount.getUnchecked(requestKey);
//            elementParams.put("count", count.toString());
//        }

        /* additional params for constructing child items */
        elementParams.put(JOURNAL_ID_KEY, journalId);

        /* write to element */
        ResolvedMenuItemDto element = new ResolvedMenuItemDto();
        element.setId(elemId);
        element.setLabel(title);
        element.setIcon(icon);
        element.setAction(JOURNAL_LINK_KEY, actionParams);
        element.setParams(elementParams);
        return element;
    }

    @Getter
    @Setter
    protected static class JournalAtts {
        private String id; //NodeRef
        private String name;
        private String title;
        @AttName("journal:journalType")
        private String journalType;
    }

    @Getter
    @RequiredArgsConstructor
    public static class IDs {
        private final String id;
        //Probably don't need this, but to keep output compatible with what menu is built by monolith ECOS,
        //original ID has to be written as well
        private final String simpleId;
    }
}
