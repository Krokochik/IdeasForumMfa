package com.krokochik.ideasforummfa.activities;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.krokochik.ideasforummfa.R;
import com.krokochik.ideasforummfa.model.Request;
import com.krokochik.ideasforummfa.model.Response;
import com.krokochik.ideasforummfa.network.HttpRequestsAddresser;
import com.krokochik.ideasforummfa.network.NetService;
import com.krokochik.ideasforummfa.resources.GV;
import com.krokochik.ideasforummfa.service.ActivityBroker;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public class LauncherActivity extends BaseActivity {

    HttpRequestsAddresser addresser = new HttpRequestsAddresser();
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1;
    SharedPreferences preferences;

    private boolean isServerAvailable() {
        try {
            Response response = addresser.sendRequest(
                    new Request(Request.Method.GET, new URL(GV.L_SERVER_ENDPOINT + "/ping")));
            return response.getCode() == 200;
        } catch (Exception e) { // if server is absolutely off HttpRequestsAddresser may produce FileNotFoundException
            return false;
        }
    }

    private boolean isSecretOk(String secret) {
//        return false;
        return secret != null &&
                !secret.trim().isEmpty() &&
                secret.length() == GV.VAL_SECRET_LENGTH;
    }

    boolean secretOk;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(GV.ST_PREF_NAME, MODE_PRIVATE);
        secretOk = isSecretOk(preferences.getString("secret", null));

        setContentView(R.layout.activity_launcher);
        if (secretOk) {
            ((TextView) findViewById(R.id.loadMessage)).setText("Пожалуйста, подождите");
        }
        launch();
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
                if (isServerAvailable()) {
                    startActivity(new Intent(this, ActivityBroker.getCurrentActivityClass()));
                } else {
                    startActivity(new Intent(this, ViewsActivity.class)
                            .putExtra("view", GV.VIEW_CONNECTING_TO_SERVER_FAILURE));
                }
            } else {
                startActivity(new Intent(this, ViewsActivity.class)
                        .putExtra("view", GV.VIEW_INTERNET_IS_UNAVAILABLE));
            }
        }, this);

        if (!NetService.isNetworkAvailable(this)) {
            startActivity(new Intent(this, ViewsActivity.class)
                    .putExtra("view", GV.VIEW_INTERNET_IS_UNAVAILABLE));
            return;
        }

        new Thread(() -> {
            if (!secretOk) {
                // checking availability of server
                boolean isServerAvailable = isServerAvailable();
                if (!isServerAvailable) {
                    startActivity(new Intent(this, ViewsActivity.class)
                            .putExtra("view", GV.VIEW_CONNECTING_TO_SERVER_FAILURE));
                    return;
                }
            }

            if (secretOk) {
                // secret presents
                startActivity(new Intent(this, MainActivity.class));
            } else {
                // secret doesn't present
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.CAMERA
                    }, CAMERA_PERMISSION_REQUEST_CODE);
                } else {
                    startActivity(new Intent(this, AuthorizationActivity.class));
                }
            }
        }).start();
    }
}