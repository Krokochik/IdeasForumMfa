package com.krokochik.ideasforummfa.activities;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import com.jaredrummler.materialspinner.MaterialSpinner;
import com.jaredrummler.materialspinner.MaterialSpinnerAdapter;
import com.jaredrummler.materialspinner.MaterialSpinnerBaseAdapter;
import com.krokochik.ideasforummfa.R;
import com.krokochik.ideasforummfa.service.ActivityBroker;
import com.krokochik.ideasforummfa.ui.Spinner;
import com.krokochik.ideasforummfa.ui.TransitionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import lombok.val;
import lombok.var;

public class GetMasterPassword extends Activity {

    Spinner accountSpinner;
    TransitionButton button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityBroker.setCurrentActivityClass(this.getClass());
        setContentView(R.layout.activity_get_master_password);

        val ctx = this;
        new Thread(() -> {
            ctx.runOnUiThread(() -> {
                ((ImageView) findViewById(R.id.imageView4)).setImageBitmap(ActivityBroker.getSender().enquireAvatar(this));
            });
        }).start();

        accountSpinner = findViewById(R.id.spinner);
        button = findViewById(R.id.button);

        accountSpinner.setItemCollection("User", "ogijdfgxpio");
        accountSpinner.getTextSize();
        accountSpinner.getItems();

        button.setOnClickListener(listener -> button.startAnimation());
    }

    @Override
    protected void onStart() {
        super.onStart();

    }
}