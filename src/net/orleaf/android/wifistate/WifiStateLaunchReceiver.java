package net.orleaf.android.wifistate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

public class WifiStateLaunchReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        String onTap = WifiStatePreferences.getActionOnTap(context);
        if (onTap.equals("toggle_wifi")) {
            if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
                if (WifiState.DEBUG) Log.d(WifiState.TAG, "Wi-Fi enabled.");
                mWifiManager.setWifiEnabled(true);
            } else {
                if (WifiState.DEBUG) Log.d(WifiState.TAG, "Wi-Fi disabled.");
                mWifiManager.setWifiEnabled(false);
            }
        } else if (onTap.equals("wifi_settings")) {
            Intent launchIntent = new Intent();
            launchIntent.setClassName("com.android.settings", "com.android.settings.wifi.WifiSettings");
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);
        } else {
            Intent launchIntent = new Intent(context, WifiStateActivity.class);
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);
        }
    }

}
