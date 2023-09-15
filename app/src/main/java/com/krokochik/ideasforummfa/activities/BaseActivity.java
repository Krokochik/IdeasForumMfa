package com.krokochik.ideasforummfa.activities;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.krokochik.ideasforummfa.service.ActivityBroker;

public class BaseActivity extends Activity {
    @Override
    public void onBackPressed() {}

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityBroker.setCurrentActivityClass(getClass());
    }
}
