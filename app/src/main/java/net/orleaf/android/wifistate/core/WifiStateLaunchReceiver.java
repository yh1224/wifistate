package net.orleaf.android.wifistate.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

import net.orleaf.android.wifistate.BuildConfig;
import net.orleaf.android.wifistate.core.preferences.WifiStatePreferences;

/**
 * ステータスバーの通知アイコンがタップされた場合の処理
 */
public class WifiStateLaunchReceiver extends BroadcastReceiver {
    private static final String TAG = WifiStateLaunchReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BuildConfig.DEBUG) Log.d(TAG, "received intent: " + intent.getAction());

        String onTap = WifiStatePreferences.getActionOnTap(context);
        if (onTap.equals("toggle_wifi")) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            // ON/OFF
            if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
                WifiStateControlService.startService(context, WifiStateControlService.ACTION_WIFI_ENABLE);
            } else {
                WifiStateControlService.startService(context, WifiStateControlService.ACTION_WIFI_DISABLE);
            }
        } else if (onTap.equals("wifi_settings")) {
            // Wi-Fi 設定
            Intent launchIntent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);
        } else if (onTap.equals("reenable_wifi")) {
            // Wi-Fi 再接続
            WifiStateControlService.startService(context, WifiStateControlService.ACTION_WIFI_REENABLE);
        } else {
            // ダイアログを開く
            Intent launchIntent = new Intent(context, WifiStateStatusActivity.class);
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);
        }
    }

}
