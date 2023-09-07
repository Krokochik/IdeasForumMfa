package com.krokochik.ideasforummfa.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.krokochik.ideasforummfa.R;

import lombok.Getter;
import lombok.Setter;
import lombok.val;

@Setter
@Getter
public class HorizontalTimer extends View implements DefaultLifecycleObserver {

    private static final String TAG = "HorizontalTimer";
    private static final String LEFT_POS_PROPERTY = "xLeftPos";
    private static final int RESET_ANIM_DURATION = 500;
    private static final String SAVED_INSTANCE_STATE = "savedInstanceState";

    @SuppressLint("ResourceAsColor")
    private @ColorInt int color = R.color.blue;
    @SuppressLint("ResourceAsColor")
    private @ColorInt int flashColor = R.color.white;

    private int borderRadius = 0;
    private int timerHeight = 16;
    private long timerDuration;
    private short flashPeriod = 1000;
    private float leftXPosition;
    private float factor;

    private RectF rectF;
    private Paint backgroundPaint;
    private OnTimerElapsedListener onTimerElapsedListener;
    private ValueAnimator transformValueAnimator;
    private ValueAnimator flashAnimator;
    private long currentPlayTime;
    private boolean viewVisible;
    private boolean timerElapsed = false;

    @FunctionalInterface
    public interface OnTimerElapsedListener {
        void onTimeElapsed(long elapsedDuration, long totalDuration);
    }

    @FunctionalInterface
    public interface OnTimerResetListener {
        void onTimerResetCompleted();
    }

    public HorizontalTimer(Context context) {
        super(context);
        init();
    }

    public HorizontalTimer(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HorizontalTimer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttributes(context, attrs, defStyleAttr);
        init();
    }

    private void initAttributes(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray array = context
                .obtainStyledAttributes(attrs, R.styleable.HorizontalTimer, defStyleAttr, 0);
        int temp;
        if ((temp = array.getColor(R.styleable.HorizontalTimer_color, -1)) != -1) {
            color = temp;
        }
        if ((temp = array.getColor(R.styleable.HorizontalTimer_flashColor, -1)) != -1) {
            flashColor = temp;
        }
        if ((temp = array.getColor(R.styleable.HorizontalTimer_borderRadius, -1)) != -1) {
            borderRadius = temp;
        }
        array.recycle();
    }

    private void init() {
        rectF = new RectF();
        setTimerElapsed(true);
        updatePaint();
        invalidate();
        if (flashPeriod > 0) {
            initFlashAnimator();
        }
    }

    private void initFlashAnimator() {
        flashAnimator = ValueAnimator.ofArgb(color, flashColor);
        flashAnimator.setDuration(flashPeriod);
        flashAnimator.setRepeatMode(ValueAnimator.REVERSE);
        flashAnimator.setRepeatCount(ValueAnimator.INFINITE);
        flashAnimator.addUpdateListener(animation -> backgroundPaint.setColor((int) animation.getAnimatedValue()));
        flashAnimator.start();
    }

    private void updatePaint() {
        backgroundPaint = new Paint();
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setStrokeWidth(0);
        backgroundPaint.setColor(color);
    }

    private void startAnimation(final long currentPlayTime) {
        float aim = getWidth() / 2f - getPaddingRight();
        transformValueAnimator = ValueAnimator.ofFloat(
                leftXPosition - getPaddingRight(), Math.max(aim, 0));
        transformValueAnimator.setDuration(timerDuration);
        transformValueAnimator.setCurrentPlayTime(currentPlayTime);
        transformValueAnimator.addUpdateListener(valueAnimator -> {
            if (viewVisible) {
                factor = (float) valueAnimator.getAnimatedValue();
                if (aim - factor <= 2) {
                    factor = aim;
                }
                invalidate();
                if (valueAnimator.getCurrentPlayTime() >= timerDuration) {
                    setTimerElapsed(true);
                }
                if (onTimerElapsedListener != null) {
                    onTimerElapsedListener.onTimeElapsed(valueAnimator.getCurrentPlayTime(), timerDuration);
                }
            }
        });
        setTimerElapsed(false);
        transformValueAnimator.start();
    }

    private void startResetAnimation(final OnTimerResetListener listener) {
        if (transformValueAnimator != null && transformValueAnimator.isRunning()) {
            transformValueAnimator.cancel();
        }

        transformValueAnimator = ValueAnimator.ofFloat(
                getWidth() / 2f - getPaddingRight(), getPaddingLeft());
        transformValueAnimator.setDuration(RESET_ANIM_DURATION);
        transformValueAnimator.addUpdateListener(valueAnimator -> {
            if (viewVisible) {
                factor = (float) valueAnimator.getAnimatedValue();
                invalidate();
            }
        });
        transformValueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setTimerElapsed(true);
                setTimerDuration(0);
                currentPlayTime = 0;
                if (listener != null) {
                    listener.onTimerResetCompleted();
                }
            }
        });

        post(() -> transformValueAnimator.start());
    }

    public void start(final long duration) {
        if (!isRunning()) {
            if (factor == 0) {
                setTimerDuration(duration);
                startAnimation(0);
            } else {
                reset(() -> start(duration));
            }
        } else {
            Log.e(TAG, "start: ", new IllegalStateException("Timer is already running."));
        }
    }

    public void reset(final OnTimerResetListener listener) {
        startResetAnimation(listener);
    }

    public void start(final long duration, final OnTimerElapsedListener listener) {
        setTimerDuration(duration);
        setOnTimerElapsedListener(listener);
        start(duration);
    }

    public void setOnTimerElapsedListener(final OnTimerElapsedListener onTimerElapsedListener) {
        this.onTimerElapsedListener = onTimerElapsedListener;
    }

    public boolean isRunning() {
        return !timerElapsed;
    }

    public void setColor(@ColorInt final int color) {
        this.color = color;
        updatePaint();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected void onDraw(Canvas canvas) {
        leftXPosition = getPaddingLeft() + factor;
        rectF.left = leftXPosition;
        rectF.right = getWidth() - getPaddingRight() - factor;
        rectF.top = getPaddingTop();
        rectF.bottom = rectF.top + timerHeight - getPaddingBottom();
        setBackground(getContext().getDrawable(R.drawable.timer_background));
        canvas.drawRoundRect(rectF, borderRadius, borderRadius, backgroundPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = getMeasuredWidth() + getPaddingLeft() + getPaddingRight();
        int desiredHeight = timerHeight + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(measureDimension(desiredWidth, widthMeasureSpec),
                measureDimension(desiredHeight, heightMeasureSpec));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (currentPlayTime != 0) {
            post(() -> startAnimation(currentPlayTime));
        }
    }

    private int measureDimension(int desiredSize, int measureSpec) {
        int specMode = View.MeasureSpec.getMode(measureSpec);
        int specSize = View.MeasureSpec.getSize(measureSpec);
        int result = desiredSize;

        switch (specMode) {
            case View.MeasureSpec.EXACTLY:
                result = specSize;
                break;
            case View.MeasureSpec.AT_MOST:
                result = Math.min(result, specSize);
                break;
            case View.MeasureSpec.UNSPECIFIED:
                result = desiredSize;
                break;
        }

        if (result < desiredSize) {
            Log.e(TAG, "The view is too small.");
        }

        return result;
    }

    public LifecycleObserver getLifecycleObserver() {
        return this;
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        viewVisible = true;
        flashAnimator.start();
        if (currentPlayTime != 0) {
            startAnimation(currentPlayTime);
        } else {
            init();
            start(1);
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        viewVisible = false;
        if (transformValueAnimator != null && transformValueAnimator.isRunning()) {
            currentPlayTime = transformValueAnimator.getCurrentPlayTime();
            transformValueAnimator.cancel();
            flashAnimator.cancel();
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(SAVED_INSTANCE_STATE, super.onSaveInstanceState());
        bundle.putLong("savedPlayTime", currentPlayTime);
        bundle.putLong("savedDuration", timerDuration);
        bundle.putFloat("savedFactor", factor);
        bundle.putFloat("savedPosition", leftXPosition);
        bundle.putBoolean("timerElapsed", timerElapsed);
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            currentPlayTime = bundle.getLong("savedPlayTime");
            timerDuration = bundle.getLong("savedDuration");
            factor = bundle.getFloat("savedFactor");
            leftXPosition = bundle.getFloat("savedPosition");
            timerElapsed = bundle.getBoolean("timerElapsed");
            state = bundle.getParcelable(SAVED_INSTANCE_STATE);
        }
        super.onRestoreInstanceState(state);
        if (currentPlayTime != 0) {
            startAnimation(currentPlayTime);
        }
    }

}
