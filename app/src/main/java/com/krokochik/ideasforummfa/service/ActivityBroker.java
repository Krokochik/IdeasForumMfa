package com.krokochik.ideasforummfa.service;

import com.krokochik.ideasforummfa.activities.LauncherActivity;
import com.krokochik.ideasforummfa.model.Token;

import lombok.Getter;
import lombok.Setter;

public class ActivityBroker {
    @Getter
    @Setter
    private static Class currentActivityClass = LauncherActivity.class;

    @Getter
    @Setter
    private static Token mfaConnectingToken = new Token();
}
