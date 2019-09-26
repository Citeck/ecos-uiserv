package ru.citeck.ecos.uiserv.config;

import java.util.Arrays;
import java.util.List;

/**
 * @author Roman Makarskiy
 */
public class UIServDefault {

    private UIServDefault() {
    }

    public static class Action {

        private Action() {
        }

        public static final List<String> DEFAULT_JOURNAL_ACTIONS = Arrays.asList("default-view", "default-download",
            "default-delete", "default-edit");

        public static final String GET_ALF_JOURNAL_URL_ENDPOINT = "/share/proxy/alfresco/api/journals/config?journalId=";
    }

}
