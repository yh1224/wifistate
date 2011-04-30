package net.orleaf.android.wifistate.core;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

/**
 * 設定画面
 */
public class WifiStatePreferencesActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private ListPreference mPrefActionOnTap;
    private EditTextPreference mPrefPingTarget;
    private NumberSeekbarPreference mPrefPingInterval;
    private NumberSeekbarPreference mPrefPingRetry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (WifiState.isFreeVersion(this)) {
            addPreferencesFromResource(R.xml.preferences_free);
        } else {
            addPreferencesFromResource(R.xml.preferences);
        }

        mPrefActionOnTap = (ListPreference) findPreference(WifiStatePreferences.PREF_ACTION_ON_TAP_KEY);
        mPrefPingTarget = (EditTextPreference) findPreference(WifiStatePreferences.PREF_PING_TARGET_KEY);
        mPrefPingInterval = (NumberSeekbarPreference) findPreference(WifiStatePreferences.PREF_PING_INTERVAL_KEY);
        mPrefPingRetry = (NumberSeekbarPreference) findPreference(WifiStatePreferences.PREF_PING_RETRY_KEY);

        updateSummary();
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
        updateService();
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
     * @return
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
        if (!WifiState.isFreeVersion(this)) {
            if (mPrefPingTarget.getText() == null || mPrefPingTarget.getText().equals("")) {
                mPrefPingTarget.setSummary(
                        getResources().getString(R.string.pref_ping_target_default));
            } else {
                mPrefPingTarget.setSummary(mPrefPingTarget.getText());
            }
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
        }
    }

    private void updateService() {
        WifiStateReceiver.clearNotificationIcon(this);
        if (WifiStatePreferences.getEnabled(this)) {
            Intent intent = new Intent().setClass(this, WifiStateReceiver.class);
            sendBroadcast(intent);
        }
    }

}
