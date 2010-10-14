package net.orleaf.android.wifistate;

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
    public static final boolean PREF_CLEARABLE_DEFAULT = true;

    public static final String PREF_CLEAR_ON_CONNECTED_KEY = "clear_on_connected";
    public static final boolean PREF_CLEAR_ON_CONNECTED_DEFAULT = true;

    public static final String PREF_CLEAR_ON_DISABLED_KEY = "clear_on_disabled";
    public static final boolean PREF_CLEAR_ON_DISABLED_DEFAULT = true;

    public static final String PREF_SHOW_DATA_NETWORK_KEY = "show_data_network";
    public static final boolean PREF_SHOW_DATA_NETWORK_DEFAULT = true;

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

}
