package com.krokochik.ideasforummfa.activities;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.krokochik.ideasforummfa.R;
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

    private WebSocket webSocket;

    private void connectToServer(boolean startSavedActivity) {
        startActivity(new Intent(this, ViewsActivity.class).putExtra("view", "connecting"));

        try {
            ClientManager clientManager = ClientManager.createClient(new ClientManager());

            int tries = 0;
            while (true) {
                try {
                    clientManager.connectToServer(webSocket = new WebSocket(), new URI("ws://ideas-forum.herokuapp.com/mfa"));
                    ActivityBroker.setWebSocket(webSocket); // Saving the websocket to other activities
                    if (startSavedActivity) {
                        startActivity(new Intent(this, ActivityBroker.getCurrentActivityClass()));
                        ActivityBroker.getSender().setWebSocket(webSocket);
                    }
                    break;
                } catch (DeploymentException ignored) {
                    Thread.sleep(1000);
                    if (++tries == 10)
                        startActivity(new Intent(this, ViewsActivity.class).putExtra("view", "server"));
                }
            }
        } catch (IOException | URISyntaxException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void connectToServer() {
        connectToServer(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityBroker.setCurrentActivityClass(LauncherActivity.class);

        NetService.setOnInternetStateChange((isConnected) -> {
            if (isConnected) {
                new Thread(() -> connectToServer(true)).start();
                startActivity(new Intent(this, ActivityBroker.getCurrentActivityClass()));
            } else
                startActivity(new Intent(this, ViewsActivity.class).putExtra("view", "inet"));
        }, this);

        if (!NetService.isNetworkAvailable(this)) {
            startActivity(new Intent(this, ViewsActivity.class).putExtra("view", "inet"));
            return;
        }

        setContentView(R.layout.activity_launcher);

        new Thread(() -> {
            val preferences = getSharedPreferences("SecretStorage", MODE_PRIVATE);  // SecretStorage keeps encrypted session key and username
            Intent intent;

            connectToServer();

            if (!preferences.contains("sessionKey") || !preferences.contains("username")) { // if data unsaved: authorize
                intent = new Intent(this, AuthActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

            } else {
                MessageSender sender = new MessageSender(webSocket,
                        preferences.getString("sessionKey", ""), preferences.getString("username", ""));
                ActivityBroker.setSender(sender);  // Saving the sender to other activities

                intent = new Intent(this, GetMasterPassword.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            }

            startActivity(intent);
        }).start();
    }
}