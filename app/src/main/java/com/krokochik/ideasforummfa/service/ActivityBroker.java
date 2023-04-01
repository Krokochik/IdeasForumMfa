package com.krokochik.ideasforummfa.service;

import android.app.Activity;

import com.krokochik.ideasforummfa.activities.LauncherActivity;
import com.krokochik.ideasforummfa.network.MessageSender;
import com.krokochik.ideasforummfa.network.WebSocket;

import lombok.Getter;
import lombok.Setter;

public class ActivityBroker {
    @Getter
    @Setter
    private static Class currentActivityClass = LauncherActivity.class;

    @Getter
    @Setter
    private static WebSocket webSocket = null;

    @Getter
    @Setter
    private static MessageSender sender = null;
}
