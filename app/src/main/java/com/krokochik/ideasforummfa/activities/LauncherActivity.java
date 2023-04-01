package com.krokochik.ideasforummfa.activities;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.krokochik.ideasforummfa.R;
import com.krokochik.ideasforummfa.model.Message;
import com.krokochik.ideasforummfa.network.MessageSender;
import com.krokochik.ideasforummfa.network.NetService;
import com.krokochik.ideasforummfa.network.WebSocket;
import com.krokochik.ideasforummfa.service.ActivityBroker;

import org.glassfish.tyrus.client.ClientManager;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import jakarta.websocket.DeploymentException;
import lombok.val;

public class LauncherActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new Thread(() -> {
            System.out.println("launcher");

            WebSocket webSocket;
            ClientManager clientManager = ClientManager.createClient(new ClientManager());
            try {
                val preferences = getSharedPreferences("SecretStorage", MODE_PRIVATE);
                Intent intent;

                if (NetService.hasConnection(this)) {
                    clientManager.connectToServer(webSocket = new WebSocket(), new URI("ws://ideas-forum.herokuapp.com/mfa"));
                    ActivityBroker.setWebSocket(webSocket);

                    if (!preferences.contains("sessionKey") || !preferences.contains("username")) {
                        System.out.println(preferences.getString("sessionKey", "null"));
                        intent = new Intent(this, AuthActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

                        Log.println(Log.DEBUG, "DBG", "1");
                    } else {
                        Log.println(Log.DEBUG, "DBG", "2");
                        MessageSender sender = new MessageSender(webSocket,
                                preferences.getString("sessionKey", ""),
                                preferences.getString("username", ""));
                        ActivityBroker.setSender(sender);

                        if (sender.isAuthenticated()) {
                            Log.println(Log.DEBUG, "DBG", "3");

                            System.out.println(preferences.getString("sessionKey", "null"));
                            intent = new Intent(this, GetMasterPassword.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        } else intent = new Intent(this, AuthActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

                    }
                } else intent = new Intent(this, InternetOff.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

                startActivity(intent);
                val ctx = this;

                NetService.setOnInternetStateChange(() -> {
                    if (NetService.hasConnection(this))
                        startActivity(new Intent(this, ActivityBroker.getCurrentActivityClass()));
                    else
                        startActivity(new Intent(this, InternetOff.class));
                }, this);
            } catch (DeploymentException | URISyntaxException | IOException ignored) {
                ignored.printStackTrace();
            }
        }).start();
    }
}