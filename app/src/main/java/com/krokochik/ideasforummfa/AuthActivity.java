package com.krokochik.ideasforummfa;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.EditText;

import com.krokochik.ideasforummfa.net.NetService;
import com.krokochik.ideasforummfa.net.WebSocket;
import com.krokochik.ideasforummfa.ui.TransitionButton;

import org.glassfish.tyrus.client.ClientManager;

import java.net.URI;
import java.net.URISyntaxException;

import jakarta.websocket.DeploymentException;


public class AuthActivity extends Activity {

    // ui
    private TransitionButton transitionButton;
    private EditText username;
    private EditText password;

    public static WebSocket webSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // net
        ClientManager clientManager = ClientManager.createClient(new ClientManager());
        try {
            clientManager.asyncConnectToServer(WebSocket.class, new URI("ws://ideas-forum.herokuapp.com/mfa"));
        } catch (DeploymentException | URISyntaxException e) {
            e.printStackTrace();
        }

        //ui
        tuneTransitionButton();
    }

    protected void tuneTransitionButton() {
        transitionButton = findViewById(R.id.login_button);
        username = findViewById(R.id.usernameEdit);
        password = findViewById(R.id.passwordEdit);

        transitionButton.setOnClickListener((l) -> { // Start the loading animation when the user tap the button
            transitionButton.startAnimation();

            final Handler handler = new Handler();
            handler.postDelayed(() -> {
                boolean isSuccessful = true;

                // Choose a stop animation if your call was succesful or not
                if (isSuccessful) {
                    transitionButton.stopAnimation(TransitionButton.StopAnimationStyle.EXPAND, () -> {
                        Intent intent = new Intent(getBaseContext(), AuthActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivity(intent);
                    });
                } else {
                    transitionButton.stopAnimation(TransitionButton.StopAnimationStyle.SHAKE, null);
                }
            }, 5000);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();

        new Thread(() -> {
            if (webSocket != null) {
                webSocket.disconnect();
                webSocket = null;
            }
        }).start();

    }
}