package com.krokochik.ideasforummfa.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.krokochik.ideasforummfa.R;
import com.krokochik.ideasforummfa.model.Request;
import com.krokochik.ideasforummfa.model.Response;
import com.krokochik.ideasforummfa.network.HttpRequestsAddresser;
import com.krokochik.ideasforummfa.resources.GV;
import com.krokochik.ideasforummfa.service.ActivityBroker;
import com.krokochik.ideasforummfa.service.crypto.Cryptographer;
import com.krokochik.ideasforummfa.ui.TransitionButton;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.HashMap;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import dev.samstevens.totp.time.NtpTimeProvider;
import lombok.val;

public class PinActivity extends BaseActivity {

    private static final HttpRequestsAddresser addresser = new HttpRequestsAddresser();
    private TransitionButton transitionButton;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_pin);
        String secret = getIntent().getStringExtra("secret");
        String pin = getIntent().getStringExtra("pin");
        if (secret == null || secret.length() != 512 || secret.trim().isEmpty())
            pin = "ERROR";
        System.out.println(secret);
        String username = getIntent().getStringExtra("username");
        String mfaToken = getIntent().getStringExtra("mfaToken");


        transitionButton = findViewById(R.id.submit_button);
        textView = findViewById(R.id.textView);

        textView.setText(textView.getText() + pin);
        transitionButton.setOnClickListener(event -> {
            transitionButton.startAnimation();
            val ctx = this;

            new Thread(() -> {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException unreachable) {
                } // significant addition. DO NOT REMOVE!!!

                Request request = new Request() {{
                    setMethod(Method.POST);
                    try {
                        setUrl(new URL(GV.L_SERVER_ENDPOINT + "/activated"));
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    setBody(new HashMap<String, String>() {{
                        put("username", username);
                        put("token", Cryptographer.encrypt("token", mfaToken, ""));
                    }});
                }};
                System.out.println(request.toString());
                Response response = addresser.sendRequest(request);
                if (response.getCode() == 200) {
                    switch (response.get("activated").toString()) {
                        case "true":
                            SharedPreferences preferences =
                                    ctx.getSharedPreferences(GV.ST_PREF_NAME, MODE_PRIVATE);
                            preferences.edit()
                                    .putString(GV.ST_SECRET, secret)
                                    .putString(GV.ST_USERNAME, username)
                                    .commit();
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
                                response = addresser.sendRequest(json,
                                        new URL(GV.L_SERVER + "/api/exec"), Request.Method.POST);
                                if ("ok".equals(response.get("status"))) {
                                    if (response.getBody().containsKey("avatar")) {
                                        preferences.edit().putString("avatar",
                                                response.get("avatar").toString()).apply();
                                    }
                                    if (response.getBody().containsKey("nickname")) {
                                        preferences.edit().putString("nickname",
                                                response.get("nickname").toString()).apply();
                                    }
                                }
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            }
                            ctx.runOnUiThread(() -> transitionButton
                                    .stopAnimation(TransitionButton.StopAnimationStyle.EXPAND, () ->
                                            ctx.startActivity(new Intent(ctx, MainActivity.class)
                                                    .setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
                                    ));
                            break;
                        case "false":
                            ctx.runOnUiThread(() -> transitionButton
                                    .stopAnimation(TransitionButton.StopAnimationStyle.SHAKE, null));
                            break;
                        default:
                            ctx.runOnUiThread(() -> transitionButton
                                    .stopAnimation(TransitionButton.StopAnimationStyle.EXPAND, () ->
                                            ctx.startActivity(new Intent(ctx, AuthorizationActivity.class)
                                                    .setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
                                    ));
                    }
                } else {
                    ctx.runOnUiThread(() -> transitionButton
                            .stopAnimation(TransitionButton.StopAnimationStyle.EXPAND, () ->
                                    ctx.startActivity(new Intent(ctx, AuthorizationActivity.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
                            ));
                }
            }).start();
        });
    }
}