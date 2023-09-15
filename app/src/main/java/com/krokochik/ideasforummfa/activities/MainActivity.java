package com.krokochik.ideasforummfa.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.krokochik.ideasforummfa.R;
import com.krokochik.ideasforummfa.model.Request;
import com.krokochik.ideasforummfa.model.Response;
import com.krokochik.ideasforummfa.network.HttpRequestsAddresser;
import com.krokochik.ideasforummfa.network.NetService;
import com.krokochik.ideasforummfa.resources.GV;
import com.krokochik.ideasforummfa.service.ActivityBroker;
import com.krokochik.ideasforummfa.service.crypto.Cryptographer;
import com.krokochik.ideasforummfa.ui.HorizontalTimer;

import java.io.BufferedInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.sql.Time;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import dev.samstevens.totp.time.NtpTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.val;

public class MainActivity extends AppCompatActivity {

    TextSwitcher switcher;
    CodeGenerator generator = new DefaultCodeGenerator(
            HashingAlgorithm.valueOf(Cryptographer.getHASHING_ALGORITHM()), GV.VAL_CODE_LENGTH
    );
    TimeProvider timeProvider;
    ScheduledExecutorService executorService;
    String secret;
    String code;
    Handler handler;

    private boolean isSecretOk(String secret) {
        return secret != null &&
                !secret.trim().isEmpty() &&
                secret.length() == GV.VAL_SECRET_LENGTH;
    }

    public Thread networking(Activity ctx) {
        return new Thread(() -> {
            try {
                timeProvider = new NtpTimeProvider("pool.ntp.org");
            } catch (UnknownHostException e) {
                Log.e("ERR", e.getMessage());
            }

            SharedPreferences preferences =
                    ctx.getSharedPreferences(GV.ST_PREF_NAME, MODE_PRIVATE);
            String username = preferences.getString("username", null);
            if (NetService.isNetworkAvailable(ctx) && username != null) {
                Response response;
                JsonObject json = new JsonObject();
                JsonObject temp = new JsonObject();
                JsonArray param = new JsonArray();
                temp.add("username", new JsonPrimitive(username));
                json.add("cmd", new JsonPrimitive("get"));
                json.add("ctx", temp);
                param.add("nickname");
                param.add("avatar");
                json.add("param", param);
                try {
                    response = new HttpRequestsAddresser().sendRequest(json,
                            new URL(GV.L_SERVER + "/api/exec"), Request.Method.POST);
                    if ("ok".equals(response.get("status"))) {
                        if (response.getBody().containsKey("avatar")) {
                            preferences.edit().putString(
                                    "avatar", response.get("avatar").toString()).apply();
                        }
                        if (response.getBody().containsKey("nickname")) {
                            preferences.edit().putString("nickname",
                                    response.get("nickname").toString()).apply();
                        }
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
            TextView nicknameView = (TextView) ctx.findViewById(R.id.nickname);
            TextView usernameView = (TextView) ctx.findViewById(R.id.username);
            ImageView avatarView = (ImageView) ctx.findViewById(R.id.avatar);

            usernameView.setText(username == null ? "unknown" : username);
            nicknameView.setText(preferences.getString("nickname", "unknown"));
            String avatar = preferences.getString("avatar","guest");
            if (!avatar.equals("guest")) {
                byte[] avatarBytes = Base64.getDecoder().decode(avatar);
                Bitmap bitmap = BitmapFactory
                        .decodeByteArray(avatarBytes, 0, avatarBytes.length);
                RoundedBitmapDrawable roundedBitmapDrawable =
                        RoundedBitmapDrawableFactory.create(getResources(), bitmap);
                roundedBitmapDrawable.setCircular(true);
                int sizeInPixels = (int) getResources().getDisplayMetrics().density * 10;
                roundedBitmapDrawable.setBounds(0, 0, sizeInPixels, sizeInPixels);

                avatarView.setImageDrawable(roundedBitmapDrawable);
            }
        });
    }

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityBroker.setCurrentActivityClass(getClass());
        handler = new Handler();
        setContentView(R.layout.activity_main);

        secret = getSharedPreferences(GV.ST_PREF_NAME, MODE_PRIVATE).getString(GV.ST_SECRET, null);
        if (!isSecretOk(secret))
            startActivity(new Intent(this, LauncherActivity.class));
        Thread networking = networking(this);
        networking.start();
        try {
            networking.join();
        } catch (InterruptedException e) {
            System.exit(-1);
        }

        switcher = findViewById(R.id.textSwitcher);
        HorizontalTimer horizontalTimer = findViewById(R.id.timer);
        executorService = Executors.newSingleThreadScheduledExecutor();

        switcher.setFactory(() -> {
            TextView t = new TextView(MainActivity.this);
            t.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            t.setTextSize(46);
            return t;
        });
        switcher.setInAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
        switcher.setOutAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));

        getLifecycle().addObserver(horizontalTimer.getLifecycleObserver());
        horizontalTimer.setOnTimerElapsedListener((elapsed, total) -> {
            if (elapsed >= total) {
                horizontalTimer.start(-500 + GV.VAL_GENERATING_PERIOD_SECONDS * 1000, (e, t) -> {
                    if (e >= t) horizontalTimer.start(-500 + GV.VAL_GENERATING_PERIOD_SECONDS * 1000);
                });
            }
        });
        val ctx = this;
        Runnable runnable = () -> {
            System.out.println("AAAASD");
            try {
                code = generator.generate(
                        secret, (long) Math.floorDiv(timeProvider.getTime(), GV.VAL_GENERATING_PERIOD_SECONDS));
            } catch (CodeGenerationException e) {
                code = "Unknown error";
            }
            System.out.println(code);
            ctx.runOnUiThread(() -> switcher.setText(code));
        };
        executorService.scheduleAtFixedRate(runnable, 0, GV.VAL_GENERATING_PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void onBackPressed() {}

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(this::finish);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.postDelayed(this::finish, 61 * 1000);
    }
}