package net.orleaf.android.wifistate;

import net.orleaf.android.AboutActivity;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

public class WifiStateActivity extends Activity {
    
    private WifiManager mWifiManager;

    // Managers
    private NetworkStateInfo mNetworkStateInfo;

    private BroadcastReceiver mConnectivityReceiver = null;
    private BroadcastReceiver mReenableReceiver = null;

    private ToggleButton mWifiToggle;
    private Button mWifiSettings;
    private Button mWifiReenable;
    private TextView mNetworkNameText;
    private TextView mNetworkStateText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_LEFT_ICON);

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        setContentView(R.layout.main);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
                R.drawable.icon);

        mNetworkNameText = (TextView) findViewById(R.id.network_name);
        mNetworkStateText = (TextView) findViewById(R.id.network_status);

        mWifiSettings = (Button) findViewById(R.id.wifi_settings);
        mWifiSettings.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClassName("com.android.settings",
                        "com.android.settings.wifi.WifiSettings");
                startActivity(intent);
            }
        });

        mWifiToggle = (ToggleButton) findViewById(R.id.wifi_toggle);
        mWifiToggle.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mWifiToggle.isChecked()) {
                    enableWifi(true);
                } else {
                    enableWifi(false);
                }
            }
        });

        mWifiReenable = (Button) findViewById(R.id.wifi_reenable);
        mWifiReenable.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
                    if (WifiState.DEBUG) Log.d(WifiState.TAG, "Wi-Fi enabled.");
                    enableWifi(true);
                } else {
                    if (WifiState.DEBUG) Log.d(WifiState.TAG, "Wi-Fi disabled.");
                    enableWifi(false);
                    mReenableReceiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
                                if (WifiState.DEBUG) Log.d(WifiState.TAG, "Wi-Fi re-enabled.");
                                mWifiManager.setWifiEnabled(true);
                                unregisterReceiver(mReenableReceiver);
                                mReenableReceiver = null;
                            }
                        }
                    }; 
                    registerReceiver(mReenableReceiver,
                        new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
                }
            }
        });

        mNetworkStateInfo = new NetworkStateInfo(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ネットワーク接続状態監視
        mConnectivityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateStatus();
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mConnectivityReceiver, filter);

        updateStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mConnectivityReceiver != null) {
            unregisterReceiver(mConnectivityReceiver);
            mConnectivityReceiver = null;
        }
    }

    /**
     * オプションメニューの生成
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    /**
     * オプションメニューの選択
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
        case R.id.menu_config:
            Intent intent = new Intent().setClass(this, WifiStatePreferencesActivity.class);
            startActivity(intent);
            break;
        case R.id.menu_about:
            intent = new Intent().setClass(this, AboutActivity.class);
            startActivity(intent);
            break;
        }
        return true;
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReenableReceiver != null) {
            unregisterReceiver(mReenableReceiver);
            mReenableReceiver = null;
        }
    }

    /**
     * 
     * @param enable true:有効化/無効化
     */
    private void enableWifi(final boolean enable) {
        // 状態が変わるまでボタンを無効化
        mWifiToggle.setEnabled(false);
        mWifiReenable.setEnabled(false);

        new AsyncTask<Object, Object, Boolean>() {
            @Override
            protected Boolean doInBackground(Object... params) {
                // これがあるとIS01で固まる
                //if (!enable) mWifiManager.disconnect();
                mWifiManager.setWifiEnabled(enable);
                return true;
            }
        }.execute();
    }

    /**
     * Wi-Fi状態情報を更新する
     */
    private void updateStatus() {
        new AsyncTask<Object, Object, Boolean>() {
            @Override
            protected Boolean doInBackground(Object... params) {
                mNetworkStateInfo.update();
                return true;
            }
            protected void onPostExecute(Boolean result) {
                if (result) {
                    updateView();
                }
            }
        }.execute();
    }

    /**
     * 表示を更新する
     */
    private void updateView() {
        mNetworkNameText.setText(mNetworkStateInfo.getNetworkName());
        mNetworkStateText.setText(mNetworkStateInfo.getDetail());
        if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
            if (WifiState.DEBUG) Log.d(WifiState.TAG, "Wi-Fi state changed to DISABLED.");
            mWifiToggle.setChecked(false);
            mWifiToggle.setEnabled(true);
            mWifiReenable.setEnabled(false);
        } else if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            if (WifiState.DEBUG) Log.d(WifiState.TAG, "Wi-Fi state changed to ENABLED.");
            mWifiToggle.setChecked(true);
            mWifiToggle.setEnabled(true);
            mWifiReenable.setEnabled(true);
        }
    }

}
