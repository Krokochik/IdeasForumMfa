package com.krokochik.ideasforummfa.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telecom.Call;

import com.krokochik.ideasforummfa.model.CallbackTask;
import com.krokochik.ideasforummfa.model.Condition;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetService {
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    public static void setOnInternetStateChange(CallbackTask<Boolean> task, Context ctx) {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        AtomicBoolean currentState = new AtomicBoolean(isNetworkAvailable(ctx));
        executorService.scheduleAtFixedRate(() -> {
            if (isNetworkAvailable(ctx) != currentState.get()) {
                task.run(!currentState.get());
            }
        }, 0, 5, TimeUnit.SECONDS);
    }
}
