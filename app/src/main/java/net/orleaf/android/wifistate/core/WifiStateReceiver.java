package net.orleaf.android.wifistate.core;

import java.util.Set;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import net.orleaf.android.wifistate.BuildConfig;
import net.orleaf.android.wifistate.R;
import net.orleaf.android.wifistate.core.preferences.WifiStatePreferences;

public class WifiStateReceiver extends BroadcastReceiver {
    private static final String TAG = WifiStateReceiver.class.getSimpleName();

    private static final int NOTIFICATIONID_ICON = 1;
    private static final String ACTION_CLEAR_NOTIFICATION = "net.orleaf.android.wifistate.CLEAR_NOTIFICATION";

    public static final String ACTION_REACHABILITY = "net.orleaf.android.wifistate.ACTION_RECHABLILITY";
    public static final String EXTRA_REACHABLE = "reachable";
    public static final String EXTRA_COUNT_OK = "ok";
    public static final String EXTRA_COUNT_NG = "ng";
    public static final String EXTRA_COUNT_TOTAL = "total";

    public static final String ACTION_PING_FAIL = "net.orleaf.android.wifistate.PING_FAIL";
    public static final String EXTRA_FAIL = "fail";

    private static NetworkStateInfo mNetworkStateInfo = null;
    private static PhoneStateListener mPhoneStateListener = null;
    private static boolean mReachable;

    /**
     * ネットワーク状態の変化によって通知アイコンを切り替える
     */
    @Override
    public void onReceive(final Context ctx, Intent intent) {
        if (BuildConfig.DEBUG) logIntent(intent);

        if (!WifiStatePreferences.getEnabled(ctx)) {
            return;
        }

        // ネットワーク状態を取得
        if (mNetworkStateInfo == null) {
            mNetworkStateInfo = new NetworkStateInfo(ctx);
        }
        if (mPhoneStateListener == null && WifiStatePreferences.getShowDataNetwork(ctx)) {
            // 3G接続状態の監視を開始
            mPhoneStateListener = new PhoneStateListener() {
                @Override
                public void onDataConnectionStateChanged(int state) {
                    super.onDataConnectionStateChanged(state);
                    if (mNetworkStateInfo != null) {
                        updateState(ctx);
                    }
                }
            };
            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
        }

        if (intent.getAction() != null) {
            if (intent.getAction().equals("android.intent.action.PACKAGE_REPLACED"/*Intent.ACTION_PACKAGE_REPLACED*/)) {
                if (intent.getData() == null ||
                    !intent.getData().equals(Uri.fromParts("package", ctx.getPackageName(), null))) {
                    return;
                }
            } else if (intent.getAction().equals(ACTION_REACHABILITY)) {
                /*
                 * ネットワーク疎通監視結果通知 (ping毎に通知)
                 */
                if (mNetworkStateInfo.isConnected()) {  // 接続中のみ
                    boolean reachable = intent.getBooleanExtra(EXTRA_REACHABLE, true);
                    if (reachable != mReachable) {
                        mReachable = reachable;
                        int iconRes;
                        if (mReachable) {
                            iconRes = mNetworkStateInfo.getIcon();
                        } else {
                            iconRes = R.drawable.state_warn;
                        }
                        String extra = null;
                        if (BuildConfig.DEBUG) {
                            int ok = intent.getIntExtra("ok", 0);
                            int total = intent.getIntExtra("total", 0);
                            extra = " ping:" + ok + "/" + total;
                        }
                        showNotificationIcon(ctx, iconRes, mNetworkStateInfo, extra);
                    }
                }
                return;
            } else if (intent.getAction().equals(ACTION_PING_FAIL)) {
                /*
                 * ネットワーク疎通監視失敗 (指定回数連続失敗時)
                 */
                if (WifiStatePreferences.getPingDisableWifiOnFail(ctx)) {
                    if (mNetworkStateInfo.isWifiConnected()) {
                        int wait = WifiStatePreferences.getPingDisableWifiPeriod(ctx);
                        WifiStateControlService.startService(ctx,
                                WifiStateControlService.ACTION_WIFI_REENABLE, wait);
                    }
                }
                return;
            } else if (intent.getAction().equals(ACTION_CLEAR_NOTIFICATION)) {
                /*
                 * 通知アイコン消去タイマ満了
                 */
                // 状態が変わっているかもしれないので再度チェックして消去可能なら消去
                if (mNetworkStateInfo.isClearableState()) {
                    clearNotification(ctx);
                    return;
                }
            }
        }

        updateState(ctx);
    }

    /**
     * ネットワーク状態情報の更新、および通知アイコンの反映
     */
    private void updateState(Context ctx) {
        if (mNetworkStateInfo.update()) {
            // 状態が変化したら通知アイコンを更新
            showNotificationIcon(ctx, mNetworkStateInfo.getIcon(), mNetworkStateInfo, null);
            //noinspection PointlessBooleanExpression
            if (!BuildConfig.LITE_VERSION) {
                if (WifiStatePreferences.getPing(ctx) &&
                        (mNetworkStateInfo.getState().equals(NetworkStateInfo.States.STATE_WIFI_CONNECTED) ||
                                (mNetworkStateInfo.getState().equals(NetworkStateInfo.States.STATE_MOBILE_CONNECTED) &&
                                        WifiStatePreferences.getPingOnMobile(ctx)))) {
                    // ネットワーク疎通監視サービス開始
                    mReachable = true;
                    WifiStatePingService.startService(ctx, mNetworkStateInfo.getGatewayIpAddress());
                } else {
                    // ネットワーク疎通監視サービス停止
                    WifiStatePingService.stopService(ctx);
                }
            }
            if (mNetworkStateInfo.isClearableState()) {
                // 3秒後に消去するタイマ
                long next = SystemClock.elapsedRealtime() + 3000;
                Intent clearIntent = new Intent(ctx, WifiStateReceiver.class).setAction(ACTION_CLEAR_NOTIFICATION);
                AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
                alarmManager.set(AlarmManager.ELAPSED_REALTIME, next, PendingIntent.getBroadcast(ctx, 0, clearIntent, PendingIntent.FLAG_CANCEL_CURRENT));
            }
        }
    }

    /**
     * 状態をクリア
     *
     * @param ctx Context
     */
    public static void disable(Context ctx) {
        if (mPhoneStateListener != null) {
            // 3G接続状態の監視を停止
            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            mPhoneStateListener = null;
        }
        // ネットワーク状態情報を破棄
        mNetworkStateInfo = null;
    }

    /**
     * ステータスバーに通知アイコンをすべて表示 (テスト用)
     */
    public static void testNotificationIcon(Context ctx) {
        int[] icons = {
            R.drawable.state_w1,
            R.drawable.state_w2,
            R.drawable.state_w3,
            R.drawable.state_w4,
            R.drawable.state_w5,
            R.drawable.state_w6,
            R.drawable.state_w7,
            R.drawable.state_w8,
            R.drawable.state_m4,
            R.drawable.state_m8,
        };
        for (int i = 0; i < icons.length; i++) {
            Intent intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, intent, 0);

            Notification notification = new NotificationCompat.Builder(ctx)
                    .setSmallIcon(icons[i])
                    .setContentTitle(ctx.getResources().getString(R.string.app_name))
                    .setContentText("State" + i)
                    .setContentIntent(contentIntent)
                    .setWhen(System.currentTimeMillis())
                    .build();

            NotificationManager notificationManager = (NotificationManager)
                    ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(i, notification);
        }
    }

    /**
     * ステータスバーに通知アイコンを表示
     *
     * @param ctx Context
     * @param iconRes 表示するアイコンのリソースID
     * @param extraMessage 表示するメッセージ
     */
    private static void showNotificationIcon(Context ctx, int iconRes, NetworkStateInfo networkStateInfo, String extraMessage) {
        Intent intent = new Intent(ctx, WifiStateLaunchService.class);
        PendingIntent contentIntent = PendingIntent.getService(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        String title;
        if (networkStateInfo.getNetworkName() != null) {
            title = networkStateInfo.getNetworkType() + ": " + networkStateInfo.getNetworkName();
        } else {
            title = ctx.getResources().getString(R.string.app_name);
        }
        String message = mNetworkStateInfo.getStateDetail();
        if (extraMessage != null) {
            message += extraMessage;
        }
        Notification notification = new NotificationCompat.Builder(ctx)
                .setSmallIcon(iconRes)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(contentIntent)
                .setWhen(System.currentTimeMillis())
                .setOngoing(!WifiStatePreferences.getClearable(ctx))
                .build();

        NotificationManager notificationManager = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATIONID_ICON, notification);
    }

    /**
     * ステータスバーの通知アイコンを消去
     */
    public static void clearNotification(Context ctx) {
        NotificationManager notificationManager =
            (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATIONID_ICON);
        disable(ctx);
    }

    /**
     * インテントのログ採取
     */
    private static void logIntent(Intent intent) {
        Log.d(TAG, "received intent: " + intent.getAction());

        Bundle extras = intent.getExtras();
        if (extras != null) {
            Set<String> keySet = extras.keySet();
            if (keySet != null) {
                Object[] keys = keySet.toArray();
                for (Object key : keys) {
                    Object o = extras.get((String) key);
                    Log.d(TAG, "  " + key + " = (" + o.getClass().getName() + ") " + o.toString());
                }
            }
        }
    }

    /**
     * ネットワーク状態の通知を開始/再開
     * 設定を変更した場合などに、明示的に表示を更新したいときに呼ぶ。
     */
    public static void startNotification(Context ctx) {
        if (WifiStatePreferences.getEnabled(ctx)) {
            // 空インテントを投げて強制的に更新
            Intent intent = new Intent().setClass(ctx, WifiStateReceiver.class);
            ctx.sendBroadcast(intent);
        } else {
            // 無効に設定された場合は消去
            clearNotification(ctx);
        }
        disable(ctx);
    }

}
