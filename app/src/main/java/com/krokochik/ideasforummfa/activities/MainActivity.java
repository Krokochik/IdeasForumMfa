package com.krokochik.ideasforummfa.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.vectordrawable.graphics.drawable.ArgbEvaluator;

import android.util.Log;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.Toast;

import com.krokochik.ideasforummfa.R;
import com.krokochik.ideasforummfa.ui.HorizontalTimer;
import com.prush.bndrsntchtimer.BndrsntchTimer;

import java.util.Random;

public class MainActivity extends AppCompatActivity {



    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        HorizontalTimer horizontalTimer = findViewById(R.id.timer);
        getLifecycle().addObserver(horizontalTimer.getLifecycleObserver());
        horizontalTimer.setOnTimerElapsedListener((elapsed, total) -> {
            if (elapsed >= total) {
                horizontalTimer.start(30_000, (e, t) -> {
                    if (e >= t) horizontalTimer.start(30_000);
                });
            }
        });

    }
}