package net.orleaf.android.wifistate.core.preferences;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import net.orleaf.android.AboutActivity;
import net.orleaf.android.wifistate.core.NetworkStateInfo;
import net.orleaf.android.wifistate.core.R;
import net.orleaf.android.wifistate.core.WifiState;
import net.orleaf.android.wifistate.core.WifiStateReceiver;

/**
 * 設定画面
 */
public class WifiStatePreferencesActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private ListPreference mPrefActionOnTap;
    private EditTextPreference mPrefPingTarget;
    private NumberSeekbarPreference mPrefPingTimeout;
    private NumberSeekbarPreference mPrefPingInterval;
    private NumberSeekbarPreference mPrefPingRetry;
    private NumberSeekbarPreference mPrefPingDisableWifiPeriod;
    private Preference mPrefWifiSettings;
    private Preference mPrefBrowseRouter;
    private Preference mPrefAbout;
    private NetworkStateInfo mNetworkStateInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (WifiState.isLiteVersion(this)) {
            addPreferencesFromResource(R.xml.preferences_free);
        } else {
            addPreferencesFromResource(R.xml.preferences);
        }

        mPrefActionOnTap = (ListPreference) findPreference(WifiStatePreferences.PREF_ACTION_ON_TAP_KEY);
        mPrefPingTarget = (EditTextPreference) findPreference(WifiStatePreferences.PREF_PING_TARGET_KEY);
        mPrefPingTimeout = (NumberSeekbarPreference) findPreference(WifiStatePreferences.PREF_PING_TIMEOUT_KEY);
        mPrefPingInterval = (NumberSeekbarPreference) findPreference(WifiStatePreferences.PREF_PING_INTERVAL_KEY);
        mPrefPingRetry = (NumberSeekbarPreference) findPreference(WifiStatePreferences.PREF_PING_RETRY_KEY);
        mPrefPingDisableWifiPeriod = (NumberSeekbarPreference) findPreference(WifiStatePreferences.PREF_PING_DISABLE_WIFI_PERIOD_KEY);
        mPrefWifiSettings = findPreference(WifiStatePreferences.PREF_WIFI_SETTINGS_KEY);
        mPrefBrowseRouter = findPreference(WifiStatePreferences.PREF_BROWSE_ROUTER);
        mPrefAbout = findPreference(WifiStatePreferences.PREF_ABOUT);

        updateSummary();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference) {
        if (preference == mPrefWifiSettings) {
            // Wi-Fi 設定
            startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
        } else if (preference == mPrefBrowseRouter) {
            // ルータ設定
            if (mNetworkStateInfo == null) {
                mNetworkStateInfo = new NetworkStateInfo(this);
            }
            mNetworkStateInfo.update();
            String gatewayAddr = mNetworkStateInfo.getGatewayIpAddress();
            if (gatewayAddr != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + gatewayAddr));
                startActivity(intent);
            }
        } else if (preference == mPrefAbout) {
            // バージョン情報
            Intent intent = new Intent().setClass(this, AboutActivity.class);
            intent.putExtra("body_asset", "about.txt");
            startActivity(intent);
        }
        return true;
    }
    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        WifiStateReceiver.startNotification(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // summaryを更新
        updateSummary();
    }

    /**
     * 設定値から表示文字列を取得
     *
     * @param val 設定値
     * @param entries　表示文字列の配列
     * @param entryvalues　設定値の配列
     * @return 表示文字列
     */
    private String getEntryString(String val, String[] entries, String[] entryvalues) {
        for (int i = 0; i < entries.length; i++) {
            if (val.equals(entryvalues[i])) {
                return entries[i];
            }
        }
        return null;
    }

    /**
     * 表示の更新
     */
    private void updateSummary() {
        mPrefActionOnTap.setSummary(
                getEntryString(mPrefActionOnTap.getValue(),
                    getResources().getStringArray(R.array.entries_action_on_tap),
                    getResources().getStringArray(R.array.entryvalues_action_on_tap)));
        if (!WifiState.isLiteVersion(this)) {
            if (mPrefPingTarget.getText() == null || mPrefPingTarget.getText().equals("")) {
                mPrefPingTarget.setSummary(
                        getResources().getString(R.string.pref_ping_target_default));
            } else {
                mPrefPingTarget.setSummary(mPrefPingTarget.getText());
            }
            mPrefPingTimeout.setSummary(
                    mPrefPingTimeout.getValue() +
                    getResources().getString(R.string.pref_ping_timeout_unit));
            mPrefPingInterval.setSummary(
                    mPrefPingInterval.getValue() +
                    getResources().getString(R.string.pref_ping_interval_unit));
            if (mPrefPingRetry.getValue() == 0) {
                mPrefPingRetry.setSummary(
                        getResources().getString(R.string.pref_ping_retry_zero));
            } else {
                mPrefPingRetry.setSummary(
                        mPrefPingRetry.getValue() +
                        getResources().getString(R.string.pref_ping_retry_unit));
            }
            mPrefPingDisableWifiPeriod.setSummary(
                    mPrefPingDisableWifiPeriod.getValue() +
                    getResources().getString(R.string.pref_ping_disable_wifi_period_unit));
        }

        if (mNetworkStateInfo == null) {
            mNetworkStateInfo = new NetworkStateInfo(this);
        }
        mNetworkStateInfo.update();
        if (mNetworkStateInfo.getGatewayIpAddress() != null) {
            mPrefBrowseRouter.setEnabled(true);
        } else {
            mPrefBrowseRouter.setEnabled(false);
        }
    }

}
