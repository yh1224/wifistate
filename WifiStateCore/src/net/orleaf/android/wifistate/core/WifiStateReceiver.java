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
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class WifiStateReceiver extends BroadcastReceiver {
    private static final int NOTIFICATIONID_ICON = 1;
    private static final String ACTION_CLEAR_NOTIFICATION = "net.orleaf.android.wifistate.CLEAR_NOTIFICATION";

    public static final String ACTION_REACHABILITY = "net.orleaf.android.wifistate.ACTION_RECHABLILITY";
    public static final String EXTRA_REACHABLE = "reachable";
    public static final String EXTRA_COUNT_OK = "ok";
    public static final String EXTRA_COUNT_NG = "ng";
    public static final String EXTRA_COUNT_TOTAL = "total";

    private static Context mCtx;
    private static NetworkStateInfo mNetworkStateInfo;
    private static MyPhoneStateListener mPhoneStateListener;
    private static TelephonyManager mTelManager;
    private static boolean mReachable;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (WifiState.DEBUG) logIntent(intent);
        mCtx = context;

        if (!WifiStatePreferences.getEnabled(mCtx)) {
            return;
        }

        if (mTelManager == null) {
            mTelManager = (TelephonyManager) mCtx.getSystemService(Context.TELEPHONY_SERVICE);
        }
        if (mNetworkStateInfo == null) {
            mNetworkStateInfo = new NetworkStateInfo(mCtx);
        }
        if (mPhoneStateListener == null) {
            if (WifiStatePreferences.getShowDataNetwork(mCtx)) {
                // 3G状態リスナ登録
                mPhoneStateListener = new MyPhoneStateListener();
                mTelManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
            }
        }

        if (intent.getAction() != null) {
            if (intent.getAction().equals("android.intent.action.PACKAGE_REPLACED"/*Intent.ACTION_PACKAGE_REPLACED*/)) {
                if (intent.getData() == null ||
                    !intent.getData().equals(Uri.fromParts("package", mCtx.getPackageName(), null))) {
                    return;
                }
            } else if (intent.getAction().equals(ACTION_REACHABILITY)) {
                if (mNetworkStateInfo.isConnected()) {  // 接続中のみ
                    boolean reachable = intent.getBooleanExtra(EXTRA_REACHABLE, true);
                    if (reachable != mReachable) {
                        mReachable = reachable;
                        String message = mNetworkStateInfo.getStateMessage();
                        if (WifiState.DEBUG) {
                            int ok = intent.getIntExtra("ok", 0);
                            int total = intent.getIntExtra("total", 0);
                            message += " ping:" + ok + "/" + total;
                        }
                        if (mReachable) {
                            showNotificationIcon(context, mNetworkStateInfo.getIcon(), message);
                        } else {
                            showNotificationIcon(context, R.drawable.state_warn, message);
                        }
                    }
                }
                return;
            } else if (intent.getAction().equals(ACTION_CLEAR_NOTIFICATION)) {
                // 状態が変わっているかもしれないので再度チェックして消去可能なら消去
                if (mNetworkStateInfo.isClearableState()) {
                    clearNotificationIcon(mCtx);
                    return;
                }
            }
        }

        updateState(mCtx);
    }

    /**
     * 3G状態監視
     */
    private class MyPhoneStateListener extends PhoneStateListener {
        @Override
        public void onDataConnectionStateChanged(int state) {
            super.onDataConnectionStateChanged(state);
            if (mNetworkStateInfo != null) {
                updateState(mCtx);
            }
        }
    };

    /**
     * 通知の更新
     *
     * @param ctx
     */
    private void updateState(Context ctx) {
        if (mNetworkStateInfo.update()) {
            showNotificationIcon(ctx, mNetworkStateInfo.getIcon(), mNetworkStateInfo.getStateMessage());
            if (!WifiState.isFreeVersion(ctx) && WifiStatePreferences.getPing(ctx)) {
                if (mNetworkStateInfo.isConnected()) {
                    mReachable = true;
                    WifiStatePingService.startService(ctx, mNetworkStateInfo.getGatewayIpAddress());
                } else {
                    WifiStatePingService.stopService(ctx);
                }
            }
            if (mNetworkStateInfo.isClearableState()) {
                // 3秒後に消去
                long next = SystemClock.elapsedRealtime() + 3000;
                Intent clearIntent = new Intent(mCtx, WifiStateReceiver.class).setAction(ACTION_CLEAR_NOTIFICATION);
                AlarmManager alarmManager = (AlarmManager) mCtx.getSystemService(Context.ALARM_SERVICE);
                alarmManager.set(AlarmManager.ELAPSED_REALTIME, next, PendingIntent.getBroadcast(ctx, 0, clearIntent, 0));
            }
        }
    }

    /**
     * 状態をクリア
     *
     * @param ctx
     */
    public static void disable(Context ctx) {
        if (mPhoneStateListener != null) {
            mTelManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            mPhoneStateListener = null;
        }
        mNetworkStateInfo = null;
    }

    /**
     * 通知バーにアイコンを表示
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
            NotificationManager notificationManager = (NotificationManager)
                    ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification = new Notification(icons[i],
                    ctx.getResources().getString(R.string.app_name), System.currentTimeMillis());
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", "com.android.settings.wifi.WifiSettings");
            PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);
            notification.setLatestEventInfo(ctx, ctx.getResources().getString(R.string.app_name),
                    "State" + i, contentIntent);
            //notification.flags |= Notification.FLAG_ONGOING_EVENT;
            notificationManager.notify(i, notification);
        }
    }

    /**
     * 通知バーにアイコンを表示
     */
    public static void showNotificationIcon(Context ctx, int iconRes, String notify_text) {
        NotificationManager notificationManager = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(iconRes,
                ctx.getResources().getString(R.string.app_name), System.currentTimeMillis());
        Intent intent = new Intent(ctx, WifiStateLaunchReceiver.class);
        PendingIntent contentIntent = PendingIntent.getBroadcast(ctx, 0, intent, 0);
        notification.setLatestEventInfo(ctx, ctx.getResources().getString(R.string.app_name),
                notify_text, contentIntent);
        notification.flags = 0;
        if (!WifiStatePreferences.getClearable(ctx)) {
            notification.flags |= (Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR);
        }
        notificationManager.notify(NOTIFICATIONID_ICON, notification);
    }

    /**
     * ノーティフィケーションバーのアイコンを消去
     */
    public static void clearNotificationIcon(Context ctx) {
        NotificationManager notificationManager =
            (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATIONID_ICON);
        disable(ctx);
    }

    private static void logIntent(Intent intent) {
        Log.d(WifiState.TAG, "received intent: " + intent.getAction());

        Bundle extras = intent.getExtras();
        if (extras != null) {
            Set<String> keySet = extras.keySet();
            if (keySet != null) {
                Object[] keys = keySet.toArray();
                for (int i = 0; i < keys.length; i++) {
                    Object o = extras.get((String)keys[i]);
                    Log.d(WifiState.TAG, "  " + (String)keys[i] + " = (" + o.getClass().getName() + ") " + o.toString());
                }
            }
        }
    }

}
