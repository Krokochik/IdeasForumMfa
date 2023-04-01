package com.krokochik.ideasforummfa.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetService {
    public static boolean hasConnection(final Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiInfo != null && wifiInfo.isConnected()) {
            return true;
        }
        wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (wifiInfo != null && wifiInfo.isConnected()) {
            return true;
        }
        wifiInfo = cm.getActiveNetworkInfo();
        if (wifiInfo != null && wifiInfo.isConnected()) {
            return true;
        }
        return false;
    }

    public static void setOnInternetStateChange(Runnable runnable, Context ctx) {
        Thread thread = new Thread(() -> {
            while (true) {
                boolean currentState = hasConnection(ctx);
                while (hasConnection(ctx) == currentState) {}
                System.out.println("state changed");
                runnable.run();
            }
        });
        thread.start();
    }
}
