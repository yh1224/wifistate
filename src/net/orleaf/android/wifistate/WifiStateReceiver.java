package net.orleaf.android.wifistate;

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
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class WifiStateReceiver extends BroadcastReceiver {
    private static final String TAG = "WifiState";
    private static final int NOTIFICATIONID_ICON = 1;

    enum States {
        STATE_DISABLED,
        STATE_WIFI_1,           // no use
        STATE_WIFI_ENABLING,
        STATE_WIFI_ENABLED,
        STATE_WIFI_SCANNING,
        STATE_WIFI_CONNECTING,
        STATE_WIFI_COMPLETED,
        STATE_WIFI_OBTAINING_IPADDR,
        STATE_WIFI_CONNECTED,
        STATE_MOBILE_CONNECTING,
        STATE_MOBILE_CONNECTED
    };

    private static boolean enabled = false;
    private static Context mCtx;

    // notify info
    private static States notifyState = States.STATE_DISABLED;
    private static String notifyMessage = null;
    private static MyPhoneStateListener mPhoneStateListener = null;

    // Managers
    private static WifiManager mWifiManager;
    private static ConnectivityManager mConManager;
    private static TelephonyManager mTelManager;

    // Wi-Fi state
    private static int wifiState = 0;
    private static boolean supplicantConnected = false;
    private static SupplicantState supplicantState = null;
    private static NetworkInfo wifiNetworkInfo = null;

    // Mobile network state
    private static int dataConnectionState;
    private static NetworkInfo dataNetworkInfo = null;

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (WifiState.DEBUG) logIntent(intent);

        if (!WifiStatePreferences.getEnabled(ctx)) {
            return;
        }

        // 初回
        if (!enabled) {
            mCtx = ctx;
            mWifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
            mConManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            mTelManager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);

            if (WifiStatePreferences.getShowDataNetwork(ctx)) {
                // 3G状態リスナ登録
                mPhoneStateListener = new MyPhoneStateListener();
                mTelManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
            }

            // 現在の状態を取得
            if (WifiState.DEBUG) Log.d(TAG, "Gathering connection state.");
            wifiState = mWifiManager.getWifiState();
            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
            supplicantState = wifiInfo.getSupplicantState();
            if (supplicantState != SupplicantState.DISCONNECTED) {
                supplicantConnected = true;
            }
            wifiNetworkInfo = mConManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            dataConnectionState = mTelManager.getDataState();
            dataNetworkInfo = mConManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            enabled = true;
            notifyState = States.STATE_DISABLED;
        }

        if (intent.getAction() != null) {
            if (intent.getAction().equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
            } else if (intent.getAction().equals("android.net.wifi.STATE_CHANGE")) {
                wifiNetworkInfo = (NetworkInfo) intent.getExtras().get(WifiManager.EXTRA_NETWORK_INFO);
            } else if (intent.getAction().equals("android.net.wifi.supplicant.CONNECTION_CHANGE")) {
                supplicantConnected = intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false);
            } else if (intent.getAction().equals("android.net.wifi.supplicant.STATE_CHANGE")) {
                supplicantState = (SupplicantState) intent.getExtras().get(WifiManager.EXTRA_NEW_STATE);
            }
        }

        updateState(ctx);
    }

    /**
     * 電話状態リスナ
     *
     */
    private class MyPhoneStateListener extends PhoneStateListener {
        /**
         * 3G状態変化
         */
        @Override
        public void onDataConnectionStateChanged(int state) {
            super.onDataConnectionStateChanged(state);
            if (WifiState.DEBUG) Log.d(TAG, "DataConnectionState changed: " + state);
            dataConnectionState = state;
            dataNetworkInfo = mConManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            updateState(mCtx);
        }
    };

    /**
     * 通知の更新
     *
     * @param ctx
     */
    private void updateState(Context ctx) {
        Resources res = ctx.getResources();
        States newState = null;

        //NetworkInfo mobileNetworkInfo = mConManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        String message = null;
        if (wifiState == WifiManager.WIFI_STATE_DISABLING ||
                wifiState == WifiManager.WIFI_STATE_DISABLED ) {
            // clear wifi state
            //wifiNetworkInfo = null;
            //supplicantConnected = false;
            //supplicantState = null;
            if (WifiStatePreferences.getShowDataNetwork(ctx)) {
                if (dataConnectionState == TelephonyManager.DATA_CONNECTING) {
                    newState = States.STATE_MOBILE_CONNECTING;
                    message = res.getString(R.string.mobile_connecting);
                } else if (dataConnectionState == TelephonyManager.DATA_CONNECTED) {
                    newState = States.STATE_MOBILE_CONNECTED;
                    message = res.getString(R.string.mobile_connected);
                } else {
                    newState = States.STATE_DISABLED;
                    message = res.getString(R.string.unavailable);
                }
            } else {
                newState = States.STATE_DISABLED;
                message = res.getString(R.string.unavailable);
            }
        } else if (wifiState == WifiManager.WIFI_STATE_ENABLING) {
            // -> enabled
            newState = States.STATE_WIFI_ENABLING;
            message = res.getString(R.string.enabling);
        } else if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
            // enabling -> enabled
            if (notifyState.compareTo(States.STATE_WIFI_ENABLED) < 0) {
                newState = States.STATE_WIFI_ENABLED;
                message = res.getString(R.string.enabled);
            }

            if (wifiNetworkInfo != null && wifiNetworkInfo.isAvailable() &&
                    wifiNetworkInfo.getState() == NetworkInfo.State.CONNECTING &&
                    wifiNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                newState = States.STATE_WIFI_OBTAINING_IPADDR;
                message = res.getString(R.string.obtaining_ipaddr);
            } else if (wifiNetworkInfo != null && wifiNetworkInfo.isAvailable() &&
                    wifiNetworkInfo.getState() == NetworkInfo.State.CONNECTED &&
                    wifiNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                newState = States.STATE_WIFI_CONNECTED;
                message = res.getString(R.string.connected);
            } else if (supplicantConnected && supplicantState != null) {
                if (supplicantState == SupplicantState.SCANNING) {
                    newState = States.STATE_WIFI_SCANNING;
                    message = res.getString(R.string.scanning);
                } else if (supplicantState == SupplicantState.ASSOCIATING) {
                    newState = States.STATE_WIFI_CONNECTING;
                    message = res.getString(R.string.associating);;
                } else if (supplicantState == SupplicantState.ASSOCIATED) {
                    newState = States.STATE_WIFI_CONNECTING;
                    message = res.getString(R.string.associated);
                } else if (supplicantState == SupplicantState.FOUR_WAY_HANDSHAKE ||
                           supplicantState == SupplicantState.GROUP_HANDSHAKE) {
                    newState = States.STATE_WIFI_CONNECTING;
                    message = res.getString(R.string.handshaking);
                } else if (supplicantState == SupplicantState.COMPLETED) {
                    newState = States.STATE_WIFI_COMPLETED;
                    message = res.getString(R.string.handshake_completed);
                } else if (supplicantState == SupplicantState.DISCONNECTED) {
                    newState = States.STATE_WIFI_SCANNING;
                    message = res.getString(R.string.disconnected);
                }
            }
        }

        if (newState == null) {
            // no change
            if (WifiState.DEBUG) Log.d(TAG, "State not recognized.");
            return;
        }

        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (newState.compareTo(States.STATE_WIFI_SCANNING) > 0 && newState.compareTo(States.STATE_WIFI_CONNECTED) <= 0/*スキャン完了済、かつ*/ &&
                wifiInfo != null && wifiInfo.getSSID() != null/*SSID情報あり、かつ*/ &&
                message != notifyMessage/*前回とメッセージが変わった場合*/) {
            // SSID
            message += " (SSID:" + wifiInfo.getSSID() + ")";
        }
        else if (newState == States.STATE_MOBILE_CONNECTED) {
            // APN
            message += " (" + dataNetworkInfo.getExtraInfo() + ")";
        }

        // notify
        if (WifiStatePreferences.getClearOnDisabled(ctx) && newState == States.STATE_DISABLED) {
            // 無効時に消去
            clearNotificationIcon(ctx);
        } else if (WifiStatePreferences.getClearOnConnected(ctx) &&
                (newState == States.STATE_WIFI_CONNECTED || newState == States.STATE_MOBILE_CONNECTED)) {
            // 接続完了時に消去
            clearNotificationIcon(ctx);
        } else if (newState != notifyState || notifyMessage == null || !message.equals(notifyMessage)) {
            if (WifiState.DEBUG) Log.d(TAG, "=>[" + newState + "] " + message);
            showNotificationIcon(ctx, newState, message);
        }
        notifyState = newState;
        notifyMessage = message;
    }

    /**
     * アイコンを取得
     *
     * @param state 状態
     * @return アイコンのリソースID
     */
    private static int getIcon(States state) {
        if (state == States.STATE_DISABLED) {
            return R.drawable.icon;
        } else if (state == States.STATE_WIFI_1) {
            return R.drawable.state_w1;
        } else if (state == States.STATE_WIFI_ENABLING) {
            return R.drawable.state_w2;
        } else if (state == States.STATE_WIFI_ENABLED) {
            return R.drawable.state_w3;
        } else if (state == States.STATE_WIFI_SCANNING) {
            return R.drawable.state_w4;
        } else if (state == States.STATE_WIFI_CONNECTING) {
            return R.drawable.state_w5;
        } else if (state == States.STATE_WIFI_COMPLETED) {
            return R.drawable.state_w6;
        } else if (state == States.STATE_WIFI_OBTAINING_IPADDR) {
            return R.drawable.state_w7;
        } else if (state == States.STATE_WIFI_CONNECTED) {
            return R.drawable.state_w8;
        } else if (state == States.STATE_MOBILE_CONNECTING) {
            return R.drawable.state_m4;
        } else if (state == States.STATE_MOBILE_CONNECTED) {
            return R.drawable.state_m8;
        }
        return 0;
    }

    /**
     * 通知状態をクリア
     *
     * @param ctx
     */
    public static void disable(Context ctx) {
        enabled = false;
        if (mPhoneStateListener != null) {
            mTelManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            mPhoneStateListener = null;
        }
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
    public static void showNotificationIcon(Context ctx, States state, String notify_text) {
        NotificationManager notificationManager = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(getIcon(state),
                ctx.getResources().getString(R.string.app_name), System.currentTimeMillis());
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.wifi.WifiSettings");
        PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, intent, 0);
        notification.setLatestEventInfo(ctx, ctx.getResources().getString(R.string.app_name),
                notify_text, contentIntent);
        notification.flags = 0;
        if (!WifiStatePreferences.getClearable(ctx)) {
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
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
