package com.krokochik.ideasforummfa.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.krokochik.ideasforummfa.R;
import com.krokochik.ideasforummfa.network.MessageSender;
import com.krokochik.ideasforummfa.network.WebSocket;
import com.krokochik.ideasforummfa.service.ActivityBroker;
import com.krokochik.ideasforummfa.service.AuthService;
import com.krokochik.ideasforummfa.ui.TransitionButton;

import org.glassfish.tyrus.client.ClientManager;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import jakarta.websocket.DeploymentException;


public class AuthActivity extends Activity {

    // ui
    private TransitionButton transitionButton;
    private EditText usernameEdit;
    private EditText passwordEdit;

    private WebSocket webSocket;
    private ClientManager clientManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityBroker.setCurrentActivityClass(this.getClass());
        setContentView(R.layout.activity_auth);

        usernameEdit = findViewById(R.id.username_input);
        passwordEdit = findViewById(R.id.password_input);
        transitionButton = findViewById(R.id.login_button);

        usernameEdit.setText(savedInstanceState == null ? "" : savedInstanceState.getString("username"));

        System.out.println("auth");
        // net
        connect();

        //ui
        tuneTransitionButton();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("username", usernameEdit.getText().toString());
    }

    private void disconnect() {
        new Thread(() -> {
            if (webSocket != null) {
                webSocket.disconnect();
                webSocket = null;
            }
            if (clientManager != null) {
                clientManager.shutdown();
                clientManager = null;
            }
            System.gc();
        }).start();
    }

    private void connect() {
        System.out.println("connect");
        new Thread(() -> {
            clientManager = ClientManager.createClient(new ClientManager());
            try {
                clientManager.connectToServer(webSocket = new WebSocket(), new URI("ws://ideas-forum.herokuapp.com/mfa"));
            } catch (DeploymentException | URISyntaxException | IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void tuneTransitionButton() {
        transitionButton.setOnClickListener((l) -> {

            if (usernameEdit.getText().toString().equals("") || passwordEdit.getText().toString().equals("")) {
                transitionButton.startShakeAnimation();
                return;
            }

            transitionButton.startAnimation();
            passwordEdit.setEnabled(false);
            passwordEdit.setForeground(ContextCompat.getDrawable(this, R.drawable.foreground));
            usernameEdit.setEnabled(false);
            usernameEdit.setForeground(ContextCompat.getDrawable(this, R.drawable.foreground));



            new Thread(() -> {
                AuthService auth = new AuthService(webSocket);

                boolean isSuccessful = auth
                        .authenticate(usernameEdit.getText().toString(), passwordEdit.getText().toString());
                System.out.println(isSuccessful);
                // Choose a stop animation if your call was successful or not

                if (isSuccessful) {
                    this.runOnUiThread(() -> {
                        transitionButton.stopAnimation(TransitionButton.StopAnimationStyle.EXPAND, () -> {
                            Intent intent = new Intent(getBaseContext(), SetMasterPassword.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                                    .putExtra("sk", auth.getSessionKey())
                                    .putExtra("username", usernameEdit.getText().toString());
                            MessageSender sender = new MessageSender(webSocket,
                                    auth.getSessionKey(),
                                    usernameEdit.getText().toString());
                            ActivityBroker.setSender(sender);
                            startActivity(intent);
                        });
                    });
                } else {
                    this.runOnUiThread(() -> {
                        passwordEdit.setEnabled(true);
                        passwordEdit.setForeground(null);
                        usernameEdit.setEnabled(true);
                        usernameEdit.setForeground(null);

                        transitionButton.stopAnimation(TransitionButton.StopAnimationStyle.SHAKE, null);
                    });
                }
            }).start();
        });
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        System.out.println("res");
        connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        disconnect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        disconnect();
    }
}