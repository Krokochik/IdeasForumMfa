package com.krokochik.ideasforummfa.resources;

// Global Strings
public class GS {
    // Storage
    public static final String ST_USERNAME_SET = "usernameSet";
    public static final String ST_SESSION_KEY_SET = "sessionKeySet";
    public static final String ST_AUTH_DATA_NAME = "SecretStorage";

    // Extras
    public static final String EXTRA_SESSION_KEY = "sessionKey";
    public static final String EXTRA_USERNAME = "username";
    public static final String EXTRA_SAVED_USERNAME = "savedUsername";

    // Links
    public static final String L_SERVER_ENDPOINT = "https://ideasforum-3e3f402d99b3.herokuapp.com/mfa";

    // Regulars
    public static final String REG_AT_LEAST_ONE_NUMBER = "((.+)?\\d+(.+)?)+";
    public static final String REG_AT_LEAST_ONE_SYMBOL = "((.+)?[_\\W](.+)?)+";

    // Views
    public static final String VIEW_CONNECTING_TO_SERVER = "connecting";
    public static final String VIEW_CONNECTING_TO_SERVER_FAILURE = "server";
    public static final String VIEW_INTERNET_IS_UNAVAILABLE = "inet";

    // Messages
    public static final String MSG_CANNOT_READ_QRCODE = "Не удается прочитать QR-код";
}
