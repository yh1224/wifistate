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

    private BroadcastReceiver mReenableReceiver;
    private Handler mHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        if (intent != null) {
            if (intent.getAction().equals(ACTION_WIFI_ENABLE)) {
                enableWifi();
            } else if (intent.getAction().equals(ACTION_WIFI_DISABLE)) {
                disableWifi();
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
    private void enableWifi() {
        enableWifi(true);
    }

    /**
     * Wi-Fi 無効化
     */
    private void disableWifi() {
        enableWifi(false);
    }

    /**
     * Wi-Fi 有効化/無効化
     *
     * @param enable true:有効化 false:無効化
     */
    private void enableWifi(boolean enable) {
        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        int state = wm.getWifiState();
        wm.setWifiEnabled(enable);
        if (enable) {
            if (WifiState.DEBUG) Log.d(WifiState.TAG, "Wi-Fi enabled.");
            if (state != WifiManager.WIFI_STATE_ENABLED) {
                Toast.makeText(this, R.string.enabling_wifi, Toast.LENGTH_SHORT).show();
            }
        } else {
            // これがあるとIS01で固まった?
            //if (!enable) mWifiManager.disconnect();
            if (WifiState.DEBUG) Log.d(WifiState.TAG, "Wi-Fi disbled.");
            if (state != WifiManager.WIFI_STATE_DISABLED) {
                Toast.makeText(this, R.string.disabling_wifi, Toast.LENGTH_SHORT).show();
            }
        }
        cancelReenable();
    }

    /**
     * Wi-Fi 再有効化 (無効化→有効化)
     *
     * @param sleep 無効化してから有効化するまでの待ち時間(分)
     */
    private void reenableWifi(final int sleep) {
        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        if (wm.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
            // すでに無効の場合はすぐに有効化
            enableWifi();
            return;
        }

        // 無効化
        if (WifiState.DEBUG) Log.d(WifiState.TAG, "Wi-Fi disabled.");
        wm.setWifiEnabled(false);
        Toast.makeText(this, R.string.disabling_wifi, Toast.LENGTH_SHORT).show();

        if (mReenableReceiver == null) {
            // 無効化完了を監視
            mReenableReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int state = intent.getIntExtra("wifi_state", -1);
                    if (WifiState.DEBUG) Log.d(WifiState.TAG, "*** wifi_state = " + state);
                    if (state == WifiManager.WIFI_STATE_DISABLED) {
                        // 無効化が完了したら時間をおいて有効化
                        if (WifiState.DEBUG) Log.d(WifiState.TAG, "Wi-Fi enable after " + sleep + " min.");
                        mHandler.postDelayed(mOnWifiChanged, sleep * 60 * 1000);
                    }
                }
            };
            registerReceiver(mReenableReceiver,
                new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
        }
    }

    private Runnable mOnWifiChanged = new Runnable() {
        @Override
        public void run() {
            if (WifiState.DEBUG) Log.d(WifiState.TAG, "Wi-Fi reenabling.");
            enableWifi();
            cancelReenable();
        }
    };

    /**
     * Wi-Fi 再有効化停止
     */
    private void cancelReenable() {
        if (WifiState.DEBUG) Log.d(WifiState.TAG, "Canceling Wi-Fi reenable.");
        mHandler.removeCallbacks(mOnWifiChanged);
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
