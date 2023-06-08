package com.krokochik.ideasforummfa.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.krokochik.ideasforummfa.R;
import com.krokochik.ideasforummfa.resources.GS;

public class ViewsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        switch (getIntent().getStringExtra("view")) {
            case GS.VIEW_INTERNET_IS_UNAVAILABLE: setContentView(R.layout.exception_internet_off); break;
            case GS.VIEW_CONNECTING_TO_SERVER : setContentView(R.layout.activity_launcher); break;
            default: setContentView(R.layout.exception_server_is_unavailable); // if server is unavailable / other exc-s
                findViewById(R.id.input_button).setOnClickListener((view) ->
                    startActivity(new Intent(this, LauncherActivity.class)));
        }
    }
}