package com.krokochik.ideasforummfa.activities;

import android.app.Activity;
import android.os.Bundle;

import com.krokochik.ideasforummfa.R;

public class InternetOff extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        System.out.println("inet");
        setContentView(R.layout.activity_internet_off);
    }
}