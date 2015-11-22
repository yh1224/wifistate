package net.orleaf.android.wifistate.core.preferences;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * 設定管理
 */
public class WifiStatePreferences
{
    public static final String PREF_ENABLED_KEY = "enabled";
    public static final boolean PREF_ENABLED_DEFAULT = true;

    public static final String PREF_CLEARABLE_KEY = "clearable";
    public static final boolean PREF_CLEARABLE_DEFAULT = false;

    public static final String PREF_CLEAR_ON_CONNECTED_KEY = "clear_on_connected";
    public static final boolean PREF_CLEAR_ON_CONNECTED_DEFAULT = false;

    public static final String PREF_CLEAR_ON_SCANNING_KEY = "clear_on_scanning";
    public static final boolean PREF_CLEAR_ON_SCANNING_DEFAULT = false;

    public static final String PREF_CLEAR_ON_DISABLED_KEY = "clear_on_disabled";
    public static final boolean PREF_CLEAR_ON_DISABLED_DEFAULT = false;

    public static final String PREF_SHOW_DATA_NETWORK_KEY = "show_data_network";
    public static final boolean PREF_SHOW_DATA_NETWORK_DEFAULT = true;

    public static final String PREF_ACTION_ON_TAP_KEY = "action_on_tap";
    public static final String PREF_ACTION_ON_TAP_DEFAULT = "open_dialog";

    public static final String PREF_PING_KEY = "ping";
    public static final boolean PREF_PING_DEFAULT = false;

    public static final String PREF_PING_ON_MOBILE_KEY = "ping_on_mobile";
    public static final boolean PREF_PING_ON_MOBILE_DEFAULT = true;

    public static final String PREF_PING_TARGET_KEY = "ping_target";
    public static final String PREF_PING_TARGET_DEAFULT = "www.google.com";

    public static final String PREF_PING_TIMEOUT_KEY = "ping_timeout";
    public static final int PREF_PING_TIMEOUT_DEFAULT = 3;

    public static final String PREF_PING_INTERVAL_KEY = "ping_interval";
    public static final int PREF_PING_INTERVAL_DEFAULT = 10;

    public static final String PREF_PING_RETRY_KEY = "ping_retry";
    public static final int PREF_PING_RETRY_DEFAULT = 3;

    public static final String PREF_PING_DISABLE_WIFI_ON_FAIL_KEY = "ping_disable_wifi_on_fail";
    public static final boolean PREF_PING_DISABLE_WIFI_ON_FAIL_DEFAULT = false;

    public static final String PREF_PING_DISABLE_WIFI_PERIOD_KEY = "ping_disable_wifi_period";
    public static final int PREF_PING_DISABLE_WIFI_PERIOD_DEFAULT = 0;

    public static boolean getEnabled(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
                WifiStatePreferences.PREF_ENABLED_KEY,
                WifiStatePreferences.PREF_ENABLED_DEFAULT);
    }

    public static boolean getClearable(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
                WifiStatePreferences.PREF_CLEARABLE_KEY,
                WifiStatePreferences.PREF_CLEARABLE_DEFAULT);
    }

    public static boolean getClearOnConnected(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
                WifiStatePreferences.PREF_CLEAR_ON_CONNECTED_KEY,
                WifiStatePreferences.PREF_CLEAR_ON_CONNECTED_DEFAULT);
    }

    public static boolean getClearOnScanning(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
                WifiStatePreferences.PREF_CLEAR_ON_SCANNING_KEY,
                WifiStatePreferences.PREF_CLEAR_ON_SCANNING_DEFAULT);
    }

    public static boolean getClearOnDisabled(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
                WifiStatePreferences.PREF_CLEAR_ON_DISABLED_KEY,
                WifiStatePreferences.PREF_CLEAR_ON_DISABLED_DEFAULT);
    }

    public static boolean getShowDataNetwork(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
                WifiStatePreferences.PREF_SHOW_DATA_NETWORK_KEY,
                WifiStatePreferences.PREF_SHOW_DATA_NETWORK_DEFAULT);
    }

    public static String getActionOnTap(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getString(
                WifiStatePreferences.PREF_ACTION_ON_TAP_KEY,
                WifiStatePreferences.PREF_ACTION_ON_TAP_DEFAULT);
    }

    public static boolean getPing(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
                WifiStatePreferences.PREF_PING_KEY,
                WifiStatePreferences.PREF_PING_DEFAULT);
    }

    public static boolean getPingOnMobile(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
                WifiStatePreferences.PREF_PING_ON_MOBILE_KEY,
                WifiStatePreferences.PREF_PING_ON_MOBILE_DEFAULT);
    }

    public static String getPingTarget(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getString(
                WifiStatePreferences.PREF_PING_TARGET_KEY,
                WifiStatePreferences.PREF_PING_TARGET_DEAFULT);
    }

    public static int getPingTimeout(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getInt(
                WifiStatePreferences.PREF_PING_TIMEOUT_KEY,
                WifiStatePreferences.PREF_PING_TIMEOUT_DEFAULT);
    }

    public static int getPingInterval(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getInt(
                WifiStatePreferences.PREF_PING_INTERVAL_KEY,
                WifiStatePreferences.PREF_PING_INTERVAL_DEFAULT);
    }

    public static int getPingRetry(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getInt(
                WifiStatePreferences.PREF_PING_RETRY_KEY,
                WifiStatePreferences.PREF_PING_RETRY_DEFAULT);
    }

    public static boolean getPingDisableWifiOnFail(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
                WifiStatePreferences.PREF_PING_DISABLE_WIFI_ON_FAIL_KEY,
                WifiStatePreferences.PREF_PING_DISABLE_WIFI_ON_FAIL_DEFAULT);
    }

    public static int getPingDisableWifiPeriod(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getInt(
                WifiStatePreferences.PREF_PING_DISABLE_WIFI_PERIOD_KEY,
                WifiStatePreferences.PREF_PING_DISABLE_WIFI_PERIOD_DEFAULT);
    }

}
