package com.krokochik.ideasforummfa.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.krokochik.ideasforummfa.R;
import com.krokochik.ideasforummfa.model.Request;
import com.krokochik.ideasforummfa.model.Response;
import com.krokochik.ideasforummfa.network.HttpRequestsAddresser;
import com.krokochik.ideasforummfa.resources.GS;
import com.krokochik.ideasforummfa.service.crypto.Cryptographer;
import com.krokochik.ideasforummfa.ui.TransitionButton;

import java.net.UnknownHostException;
import java.util.HashMap;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import dev.samstevens.totp.time.NtpTimeProvider;
import lombok.val;

public class PinActivity extends Activity {

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
                    setEndpoint("/activated");
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
                            ctx.runOnUiThread(() -> transitionButton
                                    .stopAnimation(TransitionButton.StopAnimationStyle.EXPAND, () ->
                                            ctx.startActivity(new Intent(ctx, MainActivity.class)
                                                    .setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
                                    ));
                            CodeGenerator generator = new DefaultCodeGenerator(
                                    HashingAlgorithm.valueOf(Cryptographer.getHASHING_ALGORITHM()), 9);
                            try {
                                System.out.println("new NtpTimeProvider(\"pool.ntp.org\").getTime() = " + new NtpTimeProvider("pool.ntp.org").getTime());
                                System.out.println("generator.generate(secret, new NtpTimeProvider(\"pool.ntp.org\").getTime()) = " + generator.generate(secret, Math.floorDiv(new NtpTimeProvider("pool.ntp.org").getTime(), 30)));
                            } catch (CodeGenerationException e) {
                                e.printStackTrace();
                            } catch (UnknownHostException e) {
                                e.printStackTrace();
                            }
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