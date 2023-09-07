package com.krokochik.ideasforummfa.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.github.nikartm.button.FitButton;
import com.krokochik.ideasforummfa.R;
import com.krokochik.ideasforummfa.model.Request;
import com.krokochik.ideasforummfa.model.Response;
import com.krokochik.ideasforummfa.model.Token;
import com.krokochik.ideasforummfa.network.HttpRequestsAddresser;
import com.krokochik.ideasforummfa.qrcodescanner.QrCodeActivity;
import com.krokochik.ideasforummfa.resources.GS;
import com.krokochik.ideasforummfa.service.crypto.Cryptographer;
import com.krokochik.ideasforummfa.ui.TransitionButton;

import java.util.HashMap;
import java.util.Optional;

import javax.crypto.IllegalBlockSizeException;

import lombok.val;

public class AuthorizationActivity extends Activity {

    int currentViewID;
    String codeInputText = "";
    String username;
    String mfaToken;
    final static int REQUEST_CODE_QR_SCAN = 101;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(currentViewID = R.layout.activity_input_method_selection_menu);
        logic();
    }

    @Override
    public void onBackPressed() {
        if (currentViewID == R.layout.activity_enter_code) {
            setContentView(currentViewID = R.layout.activity_input_method_selection_menu);
            logic();
        }
    }

    @Nullable
    private String[] getMfaConnectPinAndSecret(String mfaToken, String username) throws IllegalBlockSizeException {
        HttpRequestsAddresser requestsAddresser = new HttpRequestsAddresser();
        Request request = new Request(Request.Method.POST, "/confirm");
        System.out.println(mfaToken);
        request.setBody(new HashMap<String, String>() {{
            put("mfaStatus", Cryptographer.encrypt("connected", mfaToken, ""));
            put("username", username);
        }});

        Response response = requestsAddresser.sendRequest(request);
        if (response.getCode() != 200)
            return null;

        return new String[]{
                Cryptographer.decrypt(response.getBody().get("PIN").toString(), mfaToken, ""),
                Cryptographer.decrypt(response.getBody().get("secret").toString(), mfaToken, "")
        };
    }

    private Optional<String[]> getMfaTokenAndUsernameFromServer(Token connectingToken) {
        String mfaToken = "";
        String username = "";

        try {
            HttpRequestsAddresser requestsAddresser = new HttpRequestsAddresser();
            Response response = requestsAddresser.sendRequest(new Request(Request.Method.GET,
                    connectingToken.getPublicPart()));

            if (response.getCode() != 200)
                return Optional.empty();

            username = response.get("username").toString();

            mfaToken = Cryptographer.decrypt(response.get("token").toString(),
                    connectingToken.getPrivatePart(), "");
        } catch (Exception e) {
            return Optional.empty();
        }

        return Optional.of(new String[]{mfaToken, username});
    }

    @Nullable
    private String[] connectMfa(Token connectingToken) throws IllegalBlockSizeException {
        Optional<String[]> mfaTokenAndUsernameOptional = getMfaTokenAndUsernameFromServer(connectingToken);
        if (!mfaTokenAndUsernameOptional.isPresent())
            return null;
        mfaToken = mfaTokenAndUsernameOptional.get()[0]; // token
        username = mfaTokenAndUsernameOptional.get()[1]; // username
        return getMfaConnectPinAndSecret(mfaToken, username);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Toast errorToast = Toast.makeText(this, GS.MSG_CANNOT_READ_QRCODE, Toast.LENGTH_LONG);

        if (resultCode != Activity.RESULT_OK) {
            Log.d("QR", "COULD NOT GET A GOOD RESULT.");
            if (data == null)
                return;

            String result = data.getStringExtra("com.blikoon.qrcodescanner.error_decoding_image");
            if (result != null) {
                errorToast.show();
            }
        }
        if (requestCode == REQUEST_CODE_QR_SCAN) {
            if (data == null)
                return;

            //Getting the passed result
            String result = data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult");
            if (result.length() != 18) {
                errorToast.show();
                return;
            }

            val ctx = this;
            Token connectingToken = new Token(result.substring(0, 9), result.substring(9));
            new Thread(() -> {
                String pin = null;
                String secret = null;
                try {
                    String[] pinAndSecret = connectMfa(connectingToken);
                    pin = pinAndSecret[0];
                    secret = pinAndSecret[1];
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                }
                if (pin != null) {
                    ctx.startActivity(new Intent(ctx, PinActivity.class)
                            .putExtra("pin", pin)
                            .putExtra("secret", secret)
                            .putExtra("username", username)
                            .putExtra("mfaToken", mfaToken));
                }
            }).start();
        }
    }

    public void logic() {
        if (currentViewID == R.layout.activity_input_method_selection_menu) {
            FitButton toInput = findViewById(R.id.input_button);
            FitButton toScan = findViewById(R.id.scan_button);

            toInput.setOnClickListener(view -> {
                setContentView(currentViewID = R.layout.activity_enter_code);
                logic();
            });

            toScan.setOnClickListener((view) -> {
                startActivityForResult(new Intent(this, QrCodeActivity.class), REQUEST_CODE_QR_SCAN);
            });
        } else if (currentViewID == R.layout.activity_enter_code) {
            TransitionButton submitButton = findViewById(R.id.submit_button);
            EditText codeInput = findViewById(R.id.code_input);
            codeInput.setText(codeInputText);
            if (codeInputText.length() == 18) // max input length
                submitButton.setEnabled(true);
            codeInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                @Override
                public void afterTextChanged(Editable editable) {
                    codeInputText = editable.toString();
                    submitButton.setEnabled(editable.length() == 18);
                }
            });

            submitButton.setOnClickListener((view -> {
                submitButton.startAnimation();

                val ctx = this;
                new Thread(() -> {
                    String code = codeInput.getText().toString();
                    String pin = null;
                    String secret = null;
                    try {
                        String[] pinAndSecret = connectMfa(
                                new Token(code.substring(0, 9), code.substring(9)));
                        pin = pinAndSecret[0];
                        secret = pinAndSecret[1];
                    } catch (IllegalBlockSizeException e) {
                        e.printStackTrace();
                    }
                    if (pin != null) {
                        try {
                            Thread.sleep(250);
                        } catch (InterruptedException unreachable) {
                        } // significant addition. DO NOT REMOVE!!!


                        String finalPin = pin;
                        String finalSecret = secret;
                        ctx.runOnUiThread(() ->
                                submitButton.stopAnimation(TransitionButton.StopAnimationStyle.EXPAND, () -> {
                                    startActivity(new Intent(getBaseContext(), PinActivity.class)
                                            .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                                            .putExtra("pin", finalPin)
                                            .putExtra("secret", finalSecret)
                                            .putExtra("username", username)
                                            .putExtra("mfaToken", mfaToken));
                                }));
                    } else ctx.runOnUiThread(() ->
                            submitButton.stopAnimation(TransitionButton.StopAnimationStyle.SHAKE, null));
                }).start();
            }));
        }
    }
}
