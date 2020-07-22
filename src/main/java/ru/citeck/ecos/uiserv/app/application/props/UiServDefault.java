package ru.citeck.ecos.uiserv.app.application.props;

import java.util.Arrays;
import java.util.List;

/**
 * @author Roman Makarskiy
 */
public class UiServDefault {

    private UiServDefault() {
    }

    public static class Action {

        private Action() {
        }

        public static final List<String> DEFAULT_JOURNAL_ACTIONS = Arrays.asList("default-view", "default-download",
            "default-delete", "default-edit");

        public static final String GET_ALF_JOURNAL_URL_ENDPOINT = "/share/proxy/alfresco/api/journals/config?journalId=";
    }
}
