package com.krokochik.ideasforummfa;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.krokochik.ideasforummfa.net.NetService;

public class LauncherActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (NetService.hasConnection(this))
            startActivity(new Intent(this, AuthActivity.class));
        else
            startActivity(new Intent(this, InternetOff.class));

        NetService.setOnInternetStateChange(() -> {
            if (NetService.hasConnection(this))
                startActivity(new Intent(this, AuthActivity.class));
            else
                startActivity(new Intent(this, InternetOff.class));
        }, this);


    }
}