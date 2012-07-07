package net.orleaf.android.wifistate.core;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

/**
 * サービス
 */
public class WifiStateControlService extends Service {
    public static final String ACTION_WIFI_ENABLE = "net.orleaf.android.wifistate.ACTION_WIFI_ENABLE";
    public static final String ACTION_WIFI_DISABLE = "net.orleaf.android.wifistate.ACTION_WIFI_DISABLE";
    public static final String ACTION_WIFI_REENABLE = "net.orleaf.android.wifistate.ACTION_WIFI_REENABLE";

    private static final String EXTRA_SLEEP = "sleep";

    private WifiManager mWifiManager;
    private BroadcastReceiver mReenableReceiver;
    private Handler mHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        if (intent != null) {
            if (intent.getAction().equals(ACTION_WIFI_ENABLE)) {
                enableWifi(true);
            } else if (intent.getAction().equals(ACTION_WIFI_DISABLE)) {
                enableWifi(false);
            } else if (intent.getAction().equals(ACTION_WIFI_REENABLE)) {
                int sleep = intent.getIntExtra(EXTRA_SLEEP, 0);
                reenableWifi(sleep);
            }
        }
    }

    public void onDestroy() {
        cancelReenable();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    /**
     * Wi-Fi 有効化
     */
    private void enableWifi(boolean enable) {
        // これがあるとIS01で固まる
        //if (!enable) mWifiManager.disconnect();
        mWifiManager.setWifiEnabled(enable);
        if (enable) {
            if (WifiState.DEBUG) Log.d(WifiState.TAG, "Wi-Fi enabled.");
            Toast.makeText(this, R.string.enabling_wifi, Toast.LENGTH_SHORT).show();
        } else {
            if (WifiState.DEBUG) Log.d(WifiState.TAG, "Wi-Fi disbled.");
            Toast.makeText(this, R.string.disabling_wifi, Toast.LENGTH_SHORT).show();
        }
        cancelReenable();
    }

    /**
     * Wi-Fi 再有効化
     */
    private void reenableWifi(final int sleep) {
        if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
            enableWifi(true);
        } else {
            if (WifiState.DEBUG) Log.d(WifiState.TAG, "Wi-Fi disabled.");
            mWifiManager.setWifiEnabled(false);
            Toast.makeText(this, R.string.disabling_wifi, Toast.LENGTH_SHORT).show();

            if (mReenableReceiver == null) {
                mReenableReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        int state = intent.getIntExtra("wifi_state", -1);
                        if (state == WifiManager.WIFI_STATE_DISABLED) {
                            if (WifiState.DEBUG) Log.d(WifiState.TAG, "Wi-Fi enable after " + sleep + " min.");
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    enableWifi(true);
                                }}, sleep * 60 * 1000);
                        } else if (state == WifiManager.WIFI_STATE_ENABLED) {
                            cancelReenable();
                        }
                    }
                };
                registerReceiver(mReenableReceiver,
                    new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
            }
        }
    }

    /**
     * Wi-Fi 再有効化停止
     */
    private void cancelReenable() {
        if (mReenableReceiver != null) {
            unregisterReceiver(mReenableReceiver);
            mReenableReceiver = null;
        }
    }


    /**
     * Wi-Fi 再有効化処理開始
     *
     * @param ctx
     * @param action ACTION_WIFI_ENABLE / ACTION_WIFI_DISABLE / ACTION_WIFI_REENABLE
     * @param sleep 有効化までの待ち時間(ACTION_WIFI_REENABLE指定時のみ)
     * @return true:成功 false:失敗
     */
    public static boolean startService(Context ctx, String action, int sleep) {
        boolean result;
        Intent intent = new Intent(ctx, WifiStateControlService.class);
        intent.setAction(action);
        intent.putExtra(EXTRA_SLEEP, sleep);
        ComponentName name = ctx.startService(intent);
        if (name == null) {
            Log.e(WifiState.TAG, "WifiStateControlService could not start!");
            result = false;
        } else {
            if (WifiState.DEBUG) Log.d(WifiState.TAG, "WifiStateControlService started: " + name);
            result = true;
        }
        return result;
    }

    /**
     * Wi-Fi 再有効化処理開始
     *
     * @param ctx
     * @param action ACTION_WIFI_ENABLE / ACTION_WIFI_DISABLE / ACTION_WIFI_REENABLE
     * @return true:成功 false:失敗
     */
    public static boolean startService(Context ctx, String action) {
        return startService(ctx, action, 0);
    }

}
