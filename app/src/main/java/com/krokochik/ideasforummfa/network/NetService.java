package com.krokochik.ideasforummfa.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telecom.Call;

import com.krokochik.ideasforummfa.model.CallbackTask;
import com.krokochik.ideasforummfa.model.Condition;

public class NetService {
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    public static void setOnInternetStateChange(CallbackTask<Boolean> task, Context ctx) {
        Thread thread = new Thread(() -> {
            while (true) {
                boolean currentState = isNetworkAvailable(ctx);
                while (isNetworkAvailable(ctx) == currentState) {}
                task.run(!currentState);
            }
        });
        thread.start();
    }
}
