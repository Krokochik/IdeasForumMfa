package com.krokochik.ideasforummfa.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;

import com.jaredrummler.materialspinner.MaterialSpinner;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class Spinner extends MaterialSpinner {
    public Spinner(Context context) {
        super(context);
    }

    public Spinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Spinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setItemCollection(String... items) {
        for (int i = 0; i < items.length; i++) {
            items[i] =  StringUtils.repeat(' ', 5) + items[i];
        }
        super.setItems(items);
    }
}
