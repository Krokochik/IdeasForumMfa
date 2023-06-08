package com.krokochik.ideasforummfa.service;

import com.krokochik.ideasforummfa.activities.LauncherActivity;

import lombok.Getter;
import lombok.Setter;

public class ActivityBroker {
    @Getter
    @Setter
    private static Class currentActivityClass = LauncherActivity.class;
}
