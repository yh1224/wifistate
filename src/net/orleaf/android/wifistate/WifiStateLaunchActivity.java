package net.orleaf.android.wifistate;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

public class WifiStateLaunchActivity extends Activity {

    private WifiManager mWifiManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        String onTap = WifiStatePreferences.getActionOnTap(this);
        if (onTap.equals("toggle_wifi")) {
            if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
                if (WifiState.DEBUG) Log.d(WifiState.TAG, "Wi-Fi enabled.");
                mWifiManager.setWifiEnabled(true);
            } else {
                if (WifiState.DEBUG) Log.d(WifiState.TAG, "Wi-Fi disabled.");
                mWifiManager.setWifiEnabled(false);
            }
        } else if (onTap.equals("wifi_settings")) {
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", "com.android.settings.wifi.WifiSettings");
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, WifiStateActivity.class);
            startActivity(intent);
        }

        finish();
    }

}
