package net.assemble.wifistate;

import java.util.Set;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

public class WifiStateReceiver extends BroadcastReceiver {
    private static final String TAG = "WifiState";
    private static final int NOTIFICATIONID_ICON = 1;

    // notify info
    private static boolean enabled = false;
    private static int notifyState = 0;
    private static String notifyMessage = null;

    // state cache
    private static int wifiState = 0;
    private static boolean supplicantConnected = false;
    private static SupplicantState supplicantState = null;
    private static NetworkInfo networkInfo = null;

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (WifiState.DEBUG) logIntent(intent);
        WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        ConnectivityManager conManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        Resources res = ctx.getResources();
        int newState = -1;

        // 初回は現在の状態を取得する
        if (!enabled) {
            wifiState = wifiManager.getWifiState();
            supplicantState = wifiInfo.getSupplicantState();
            if (supplicantState != SupplicantState.DISCONNECTED) {
                supplicantConnected = true;
            }
            networkInfo = conManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            enabled = true;
        }

        String message = null;
        if (intent.getAction().equals("android.net.wifi.WIFI_STATE_CHANGED")) {
            wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
        } else if (intent.getAction().equals("android.net.wifi.STATE_CHANGE")) {
            networkInfo = (NetworkInfo) intent.getExtras().get(WifiManager.EXTRA_NETWORK_INFO);
        } else if (intent.getAction().equals("android.net.wifi.supplicant.CONNECTION_CHANGE")) {
            supplicantConnected = intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false);
        } else if (intent.getAction().equals("android.net.wifi.supplicant.STATE_CHANGE")) {
            supplicantState = (SupplicantState) intent.getExtras().get(WifiManager.EXTRA_NEW_STATE);
        }

        if (wifiState == WifiManager.WIFI_STATE_DISABLING ||
                wifiState == WifiManager.WIFI_STATE_DISABLED ) {
            // clear all state
            networkInfo = null;
            supplicantConnected = false;
            supplicantState = null;
            newState = 0;
        } else if (wifiState == WifiManager.WIFI_STATE_ENABLING) {
            newState = 2;
            message = res.getString(R.string.enabling);
        } else if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
            if (notifyState < 2) {
                newState = 3;
                message = res.getString(R.string.enabled);
            }
        }

        if (networkInfo != null && networkInfo.isAvailable() &&
                networkInfo.getState() == NetworkInfo.State.CONNECTING &&
                networkInfo.getDetailedState() == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
            newState = 7;
            message = res.getString(R.string.obtaining_ipaddr);
        } else if (networkInfo != null && networkInfo.isAvailable() &&
                networkInfo.getState() == NetworkInfo.State.CONNECTED &&
                networkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
            newState = 8;
            message = res.getString(R.string.connected);
        } else if (supplicantState != null) {
            if (supplicantState == SupplicantState.SCANNING) {
                newState = 4;
                message = res.getString(R.string.scanning);
            } else if (supplicantState == SupplicantState.ASSOCIATING) {
                newState = 5;
                message = res.getString(R.string.associating);;
            } else if (supplicantState == SupplicantState.ASSOCIATED) {
                newState = 5;
                message = res.getString(R.string.associated);
            } else if (supplicantState == SupplicantState.FOUR_WAY_HANDSHAKE ||
                       supplicantState == SupplicantState.GROUP_HANDSHAKE) {
                newState = 5;
                message = res.getString(R.string.handshaking);
            } else if (supplicantState == SupplicantState.COMPLETED) {
                newState = 6;
                message = res.getString(R.string.handshake_completed);
            } else if (supplicantState == SupplicantState.DISCONNECTED) {
                newState = 4;
                message = res.getString(R.string.disconnected);
            }
        } else if (supplicantConnected) {
            newState = 5;
            message = notifyMessage;
        }
 
        if (newState < 0) {
            // no change
            return;
        }

        if (newState > 4/*スキャン完了済、かつ*/ &&
                wifiInfo != null && wifiInfo.getSSID() != null/*SSID情報あり、かつ*/ &&
                message != notifyMessage/*前回とメッセージが変わった場合*/) {
            message += " (SSID:" + wifiInfo.getSSID() + ")";
        }

        // notify
        if (newState == 0) {
            clearNotificationIcon(ctx);
        } else if (newState != notifyState || notifyMessage == null || !message.equals(notifyMessage)) {
            Log.d(TAG, "=>[" + newState + "] " + message);
            showNotificationIcon(ctx, newState, message);
        }
        notifyState = newState;
        notifyMessage = message;
    }

    private static int getIcon(int state) {
        if (state == 0) {
            return R.drawable.icon;
        } else if (state == 1) {
            return R.drawable.state1;
        } else if (state == 2) {
            return R.drawable.state2;
        } else if (state == 3) {
            return R.drawable.state3;
        } else if (state == 4) {
            return R.drawable.state4;
        } else if (state == 5) {
            return R.drawable.state5;
        } else if (state == 6) {
            return R.drawable.state6;
        } else if (state == 7) {
            return R.drawable.state7;
        } else if (state == 8) {
            return R.drawable.state8;
        }
        return 0;
    }

    /**
     * 通知バーにアイコンを表示
     */
    public static void testNotificationIcon(Context ctx) {
        for (int i = 1; i <= 8; i++) {
            NotificationManager notificationManager = (NotificationManager) 
                    ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification = new Notification(getIcon(i),
                    ctx.getResources().getString(R.string.app_name), System.currentTimeMillis());
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", "com.android.settings.wifi.WifiSettings");
            PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, intent, 0);
            notification.setLatestEventInfo(ctx, ctx.getResources().getString(R.string.app_name),
                    "State" + i, contentIntent);
            //notification.flags |= Notification.FLAG_ONGOING_EVENT;
            notificationManager.notify(i, notification);
        }
    }

    /**
     * 通知バーにアイコンを表示
     */
    public static void showNotificationIcon(Context ctx, int state, String notify_text) {
        NotificationManager notificationManager = (NotificationManager) 
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(getIcon(state),
                ctx.getResources().getString(R.string.app_name), System.currentTimeMillis());
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.wifi.WifiSettings");
        PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, intent, 0);
        notification.setLatestEventInfo(ctx, ctx.getResources().getString(R.string.app_name),
                notify_text, contentIntent);
        //notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notificationManager.notify(NOTIFICATIONID_ICON, notification);
    }

    /**
     * ノーティフィケーションバーのアイコンを消去
     */
    public static void clearNotificationIcon(Context ctx) {
        NotificationManager notificationManager =
            (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATIONID_ICON);
    }

    private static void logIntent(Intent intent) {
        Log.d(TAG, "received intent: " + intent.getAction());

        Bundle extras = intent.getExtras();
        if (extras != null) {
            Set<String> keySet = extras.keySet();
            if (keySet != null) {
                Object[] keys = keySet.toArray();
                for (int i = 0; i < keys.length; i++) {
                    String val;
                    Object o = extras.get((String)keys[i]);
                    if (o instanceof Integer) {
                        val = "Integer:" + ((Integer)o).toString();
                    } else if (o instanceof Boolean) {
                        val = "Boolean:" + ((Boolean)o).toString();
                    } else if (o instanceof SupplicantState) {
                        val = "SupplicantState:" + ((SupplicantState)o).toString();
                    } else if (o instanceof NetworkInfo) {
                        val = "NetworkInfo:" + ((NetworkInfo)o).toString();
                    } else if (o instanceof String) {
                        val = "String:" + (String)o;
                    } else {
                        val = o.getClass().getName() + ":?";
                    }
                    Log.d(TAG, "  " + (String)keys[i] + " = " + val);
                }
            }
        }
    }

}
