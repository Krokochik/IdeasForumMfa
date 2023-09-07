package com.krokochik.ideasforummfa.activities;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.krokochik.ideasforummfa.R;
import com.krokochik.ideasforummfa.model.Request;
import com.krokochik.ideasforummfa.model.Response;
import com.krokochik.ideasforummfa.network.HttpRequestsAddresser;
import com.krokochik.ideasforummfa.network.NetService;
import com.krokochik.ideasforummfa.resources.GS;
import com.krokochik.ideasforummfa.service.ActivityBroker;

import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicBoolean;

public class LauncherActivity extends Activity {

    HttpRequestsAddresser addresser = new HttpRequestsAddresser();
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1;

    private boolean isServerAvailable() {
        try {
            Response response = addresser.sendRequest(
                    new Request(Request.Method.GET, "/ping"));
            return response.getCode() == 200;
        } catch (Exception e) { // if server is absolutely off HttpRequestsAddresser may produce FileNotFoundException
            return false;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityBroker.setCurrentActivityClass(LauncherActivity.class);
        setContentView(R.layout.activity_launcher);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA
            }, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            launch();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launch();
        } else {
            finish();
        }
    }

    private void launch() {
        // internet connection listener
        NetService.setOnInternetStateChange((isConnected) -> {
            if (isConnected) {
                if (isServerAvailable())
                    startActivity(new Intent(this, ActivityBroker.getCurrentActivityClass()));
                else
                    startActivity(new Intent(this, ViewsActivity.class)
                            .putExtra("view", GS.VIEW_CONNECTING_TO_SERVER_FAILURE));
            } else
                startActivity(new Intent(this, ViewsActivity.class)
                        .putExtra("view", GS.VIEW_INTERNET_IS_UNAVAILABLE));
        }, this);

        if (!NetService.isNetworkAvailable(this)) {
            startActivity(new Intent(this, ViewsActivity.class)
                    .putExtra("view", GS.VIEW_INTERNET_IS_UNAVAILABLE));
            return;
        }

        new Thread(() -> {
            // checking availability of server
            boolean isServerAvailable = isServerAvailable();
            if (!isServerAvailable) {
                startActivity(new Intent(this, ViewsActivity.class)
                        .putExtra("view", GS.VIEW_CONNECTING_TO_SERVER_FAILURE));
                return;
            }

            startActivity(new Intent(this, AuthorizationActivity.class));
        }).start();
    }
}