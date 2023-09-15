package com.krokochik.ideasforummfa.resources;

// Global values
public class GV {
    // Properties
    public static final Short VAL_SECRET_LENGTH = 512;
    public static final Byte VAL_CODE_LENGTH = 9;
    public static final Byte VAL_GENERATING_PERIOD_SECONDS = 30;

    // Storage
    public static final String ST_PREF_NAME = "SecureStorage";
    public static final String ST_SECRET = "secret";
    public static final String ST_USERNAME = "username";

    // Extras
    public static final String EXTRA_SESSION_KEY = "sessionKey";
    public static final String EXTRA_USERNAME = "username";
    public static final String EXTRA_SAVED_USERNAME = "savedUsername";

    // Links
    public static final String L_SERVER_ENDPOINT = "https://ideasforum-3e3f402d99b3.herokuapp.com/mfa";
    public static final String L_SERVER = "https://ideasforum-3e3f402d99b3.herokuapp.com";

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
