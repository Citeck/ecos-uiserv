package ru.citeck.ecos.uiserv.app.application.constants;

/**
 * Application constants.
 */
public final class AppConstants {

    public final static String APP_NAME = "uiserv";

    // Regex for acceptable logins
    public static final String LOGIN_REGEX = "^[_.@A-Za-z0-9-]*$";

    public static final String SYSTEM_ACCOUNT = "system";
    public static final String ANONYMOUS_USER = "anonymoususer";
    public static final String DEFAULT_LANGUAGE = "en";

    private AppConstants() {
    }
}
