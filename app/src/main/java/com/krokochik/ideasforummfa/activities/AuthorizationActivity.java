package com.krokochik.ideasforummfa.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

import androidx.annotation.Nullable;

import com.github.nikartm.button.FitButton;
import com.krokochik.ideasforummfa.R;
import com.krokochik.ideasforummfa.qrcodescanner.QrCodeActivity;
import com.krokochik.ideasforummfa.ui.TransitionButton;

import lombok.val;

public class AuthorizationActivity extends Activity {

    int currentViewID;
    String codeInputText = "";
    final static int REQUEST_CODE_QR_SCAN = 101;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(currentViewID = R.layout.activity_input_method_selection_menu);
        addLogic();

    }

    @Override
    public void onBackPressed() {
        if (currentViewID == R.layout.activity_enter_code) {
            setContentView(currentViewID = R.layout.activity_input_method_selection_menu);
            addLogic();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != Activity.RESULT_OK)
        {
            Log.d("QR","COULD NOT GET A GOOD RESULT.");
            if (data == null)
                return;

            //Getting the passed result
            String result = data.getStringExtra("com.blikoon.qrcodescanner.error_decoding_image");
            if(result != null)
            {
                // QR code could not be scanned
            }
        }
        if (requestCode == REQUEST_CODE_QR_SCAN)
        {
            if(data==null)
                return;

            //Getting the passed result
            String result = data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult");
            Log.d("QR","Have scan result in your app activity: " + result);
        }
    }

    public void addLogic() {
        if (currentViewID == R.layout.activity_input_method_selection_menu) {
            FitButton toInput = findViewById(R.id.input_button);
            FitButton toScan = findViewById(R.id.scan_button);

            toInput.setOnClickListener(view -> {
                System.out.println("clicked");
                setContentView(currentViewID = R.layout.activity_enter_code);
                addLogic();
            });

            toScan.setOnClickListener((view) -> {
                startActivityForResult(new Intent(this, QrCodeActivity.class), REQUEST_CODE_QR_SCAN);
            });
        } else if (currentViewID == R.layout.activity_enter_code) {
            TransitionButton submitButton = findViewById(R.id.submit_button);
            EditText codeInput = findViewById(R.id.code_input);
            codeInput.setText(codeInputText);
            codeInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    codeInputText = editable.toString();
                    submitButton.setEnabled(editable.length() == 18);
                }});

            submitButton.setOnClickListener((view -> {
                submitButton.startAnimation();

                val ctx = this;
                new Thread(() -> {
                    if (true) {

                        try { Thread.sleep(250); } catch (InterruptedException unreachable) {} // significant addition. DO NOT REMOVE!!!


                        ctx.runOnUiThread(() ->
                                submitButton.stopAnimation(TransitionButton.StopAnimationStyle.EXPAND, () -> {
                                    startActivity(new Intent(getBaseContext(), MainActivity.class)
                                            .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                                }));
                    } else ctx.runOnUiThread(() ->
                            submitButton.stopAnimation(TransitionButton.StopAnimationStyle.SHAKE, null));
                }).start();
            }));
        }
    }
}
