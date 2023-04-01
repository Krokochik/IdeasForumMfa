package com.krokochik.ideasforummfa.activities;

import android.app.Activity;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.krokochik.ideasforummfa.R;
import com.krokochik.ideasforummfa.model.Condition;
import com.krokochik.ideasforummfa.service.ActivityBroker;
import com.krokochik.ideasforummfa.service.crypto.MessageCipher;
import com.krokochik.ideasforummfa.ui.TransitionButton;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

public class SetMasterPassword extends Activity {

    private TransitionButton button;
    private EditText passwordInput;

    private ArrayList<State> states;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityBroker.setCurrentActivityClass(this.getClass());
        setContentView(R.layout.activity_set_master_password);

        button = findViewById(R.id.installButton);
        passwordInput = findViewById(R.id.master_password_input);
        val ctx = this;

        states = new ArrayList<State>() {{
            add(new State(ctx, findViewById(R.id.stateOne), txt -> {
                int i = 0;
                for (char ch : txt.toCharArray()) {
                    if (Character.isUpperCase(ch)) i++;
                    if (i == 2) return true;
                }
                return false;
            }));
            add(new State(ctx, findViewById(R.id.stateTwo), txt -> {
                int i = 0;
                for (char ch : txt.toCharArray()) {
                    if (Character.isLowerCase(ch)) i++;
                    if (i == 2) return true;
                }
                return false;
            }));
            add(new State(ctx, findViewById(R.id.stateThree), txt -> txt.matches("((.+)?\\d+(.+)?)+")));
            add(new State(ctx, findViewById(R.id.stateFour), txt -> txt.matches("((.+)?[_\\W](.+)?)+")));
        }};

        button.setEnabled(false);

        passwordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                val text = passwordInput.getText().toString();
                AtomicBoolean conditionsMet = new AtomicBoolean(true);
                states.forEach(state -> {
                    state.checkAndDraw(text);
                    if (!state.isMet())
                        conditionsMet.set(false);
                });

                button.setEnabled(conditionsMet.get());
            }
        });

        button.setOnClickListener((listener) -> {
            button.startAnimation();

            val preferences = getSharedPreferences("SecretStorage", MODE_PRIVATE);
            val encryptedKey = MessageCipher.encrypt(getIntent().getExtras().get("sk").toString(), "", passwordInput.getText().toString());
            System.out.println("encryptedKey = " + encryptedKey);

            if (preferences.edit().putString("sessionKey", encryptedKey).commit() &&
                    preferences.edit().putString("username", getIntent().getExtras().get("username").toString()).commit())
                this.runOnUiThread(() -> {
                    button.stopAnimation(TransitionButton.StopAnimationStyle.EXPAND, null);
                });
            else this.runOnUiThread(() -> button.stopAnimation(TransitionButton.StopAnimationStyle.SHAKE, null));
        });
    }

    @RequiredArgsConstructor
    @NoArgsConstructor
    private static class State {
        @Getter
        private boolean met = false;
        @NonNull
        private Activity ctx;
        @NonNull
        private ImageView view;
        @NonNull
        Condition<String> condition;

        public void checkAndDraw(String text) {
            boolean state = condition.check(text);

            if (met) {
                if (!state) {
                    view.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.avd_check_to_close));
                    ((Animatable) view.getDrawable()).start();
                }
            } else {
                if (state) {
                    view.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.avd_close_to_check));
                    ((Animatable) view.getDrawable()).start();
                }
            }
            this.met = state;
        }
    }

    @SneakyThrows
    @Override
    protected void onStart() {
        super.onStart();
    }
}