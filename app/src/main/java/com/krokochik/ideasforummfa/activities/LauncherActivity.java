package com.krokochik.ideasforummfa.activities;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.krokochik.ideasforummfa.R;
import com.krokochik.ideasforummfa.network.MessageSender;
import com.krokochik.ideasforummfa.network.NetService;
import com.krokochik.ideasforummfa.network.WebSocket;
import com.krokochik.ideasforummfa.resources.GS;
import com.krokochik.ideasforummfa.service.ActivityBroker;

import org.glassfish.tyrus.client.ClientManager;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;

import jakarta.websocket.DeploymentException;
import lombok.val;

public class LauncherActivity extends Activity {

    private WebSocket webSocket;

    private void connectToServer(boolean startSavedActivity) {
        startActivity(new Intent(this, ViewsActivity.class).putExtra("view", GS.VIEW_CONNECTING_TO_SERVER));

        try {
            ClientManager clientManager = ClientManager.createClient(new ClientManager());

            int tries = 0;
            while (true) {
                try {
                    clientManager.connectToServer(webSocket = new WebSocket(), new URI(GS.L_SERVER_WEBSOCKET_ENDPOINT));
                    webSocket.setOnCloseEvent(reason -> {
                        if (NetService.isNetworkAvailable(this))
                            startActivity(new Intent(this, ViewsActivity.class).putExtra("view", GS.VIEW_CONNECTING_TO_SERVER_FAILURE));
                    });
                    ActivityBroker.setWebSocket(webSocket); // Saving the websocket to other activities
                    if (startSavedActivity) {
                        startActivity(new Intent(this, ActivityBroker.getCurrentActivityClass()));
                        ActivityBroker.getSender().setWebSocket(webSocket);
                    }
                    break;
                } catch (DeploymentException ignored) {
                    Thread.sleep(1000);
                    if (++tries == 10)
                        if (NetService.isNetworkAvailable(this))
                            startActivity(new Intent(this, ViewsActivity.class).putExtra("view", GS.VIEW_CONNECTING_TO_SERVER_FAILURE));
                        else break;
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
                startActivity(new Intent(this, ViewsActivity.class).putExtra("view", GS.VIEW_INTERNET_IS_UNAVAILABLE));
        }, this);

        if (!NetService.isNetworkAvailable(this)) {
            startActivity(new Intent(this, ViewsActivity.class).putExtra("view", GS.VIEW_INTERNET_IS_UNAVAILABLE));
            return;
        }

        setContentView(R.layout.activity_launcher);

        new Thread(() -> {
            val preferences = getSharedPreferences(GS.ST_AUTH_DATA_NAME, MODE_PRIVATE);  // SecretStorage keeps encrypted session key and username
            Intent intent;

            connectToServer();

            val usernameSet = new HashSet<>(preferences.getStringSet(GS.ST_USERNAME_SET, new HashSet<>()));
            val sessionKeySet = new HashSet<>(preferences.getStringSet(GS.ST_SESSION_KEY_SET, new HashSet<>()));

            boolean needToAuthorize = usernameSet.size() != sessionKeySet.size();

            String username = "";
            String sessionKey = "";
            if (!needToAuthorize) {
                for (String name : usernameSet) {
                    for (String key : sessionKeySet) {
                        // 4 is min base64 output length
                        // and min username length on ideas-forum
                        if ((name.length() >= 4) && (key.length() >= 4)) {
                            username = name;
                            sessionKey = key;
                        }
                    }
                }
            }

            if (username.equals(""))
                needToAuthorize = true;

            if (needToAuthorize) {
                intent = new Intent(this, AuthActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            } else {
                MessageSender sender = new MessageSender(webSocket, sessionKey, username);
                ActivityBroker.setSender(sender);  // Saving the sender to other activities

                intent = new Intent(this, GetMasterPassword.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        .putExtra(GS.EXTRA_USERNAME, username)
                        .putExtra(GS.EXTRA_SESSION_KEY, sessionKey);
            }

            startActivity(intent);
        }).start();
    }
}