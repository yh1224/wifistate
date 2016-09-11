package net.orleaf.android.wifistate.core;

import android.app.IntentService;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

import net.orleaf.android.wifistate.BuildConfig;
import net.orleaf.android.wifistate.core.preferences.WifiStatePreferences;

/**
 * ステータスバーの通知アイコンがタップされた場合の処理
 */
public class WifiStateLaunchService extends IntentService {
    private static final String TAG = WifiStateLaunchService.class.getSimpleName();

    public WifiStateLaunchService(String name) {
        super(name);
    }

    @SuppressWarnings("unused")
    public WifiStateLaunchService() {
        this(TAG);
    }

    @Override
    public void onHandleIntent(Intent intent) {
        if (BuildConfig.DEBUG) Log.d(TAG, "received intent: " + intent.getAction());

        String onTap = WifiStatePreferences.getActionOnTap(this);
        if (onTap.equals("toggle_wifi")) {
            WifiManager wifiManager = (WifiManager) this.getSystemService(WIFI_SERVICE);
            // ON/OFF
            if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
                WifiStateControlService.startService(this, WifiStateControlService.ACTION_WIFI_ENABLE);
            } else {
                WifiStateControlService.startService(this, WifiStateControlService.ACTION_WIFI_DISABLE);
            }
        } else if (onTap.equals("wifi_settings")) {
            // Wi-Fi 設定
            Intent launchIntent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(launchIntent);
        } else if (onTap.equals("reenable_wifi")) {
            // Wi-Fi 再接続
            WifiStateControlService.startService(this, WifiStateControlService.ACTION_WIFI_REENABLE);
        } else {
            // ダイアログを開く
            Intent launchIntent = new Intent(this, WifiStateStatusActivity.class);
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(launchIntent);
        }
    }
}
