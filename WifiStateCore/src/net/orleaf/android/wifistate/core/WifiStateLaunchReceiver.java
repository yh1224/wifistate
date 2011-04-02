package net.orleaf.android.wifistate.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

public class WifiStateLaunchReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        String onTap = WifiStatePreferences.getActionOnTap(context);
        if (onTap.equals("toggle_wifi")) {
            if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
                WifiStateControlService.startSerivce(context, WifiStateControlService.ACTION_WIFI_ENABLE);
            } else {
                WifiStateControlService.startSerivce(context, WifiStateControlService.ACTION_WIFI_DISABLE);
            }
        } else if (onTap.equals("wifi_settings")) {
            Intent launchIntent = new Intent();
            launchIntent.setClassName("com.android.settings", "com.android.settings.wifi.WifiSettings");
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);
        } else if (onTap.equals("reenable_wifi")) {
            WifiStateControlService.startSerivce(context, WifiStateControlService.ACTION_WIFI_REENABLE);
        } else {
            Intent launchIntent = new Intent();
            launchIntent.setClassName(context.getPackageName(), context.getPackageName() + ".WifiStateActivity");
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);
        }
    }

}
