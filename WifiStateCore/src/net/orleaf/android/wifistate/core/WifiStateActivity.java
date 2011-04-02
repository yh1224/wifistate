package net.orleaf.android.wifistate.core;

import java.util.List;

import net.orleaf.android.AboutActivity;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

public class WifiStateActivity extends Activity {

    // Managers
    private WifiManager mWifiManager;

    private NetworkStateInfo mNetworkStateInfo = null;
    private List<WifiConfiguration> mNetworkList = null;

    private BroadcastReceiver mConnectivityReceiver = null;

    private ToggleButton mWifiToggle;
    private Button mWifiSettings;
    private Button mWifiReenable;
    private LinearLayout mNetworkLayout;
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

        mNetworkLayout = (LinearLayout) findViewById(R.id.network);
        registerForContextMenu(mNetworkLayout);
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
                    enableWifi(WifiStateControlService.ACTION_WIFI_ENABLE);
                } else {
                    enableWifi(WifiStateControlService.ACTION_WIFI_DISABLE);
                }
            }
        });

        mWifiReenable = (Button) findViewById(R.id.wifi_reenable);
        mWifiReenable.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                enableWifi(WifiStateControlService.ACTION_WIFI_REENABLE);
            }
        });
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
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        menu.setHeaderTitle(R.string.connect_to);
        // SSID一覧
        if (mNetworkList == null) {
            mNetworkList = mWifiManager.getConfiguredNetworks();
        }
        for (int i = 0; i < mNetworkList.size(); i++) {
            WifiConfiguration config = mNetworkList.get(i);
            String ssid = config.SSID;
            if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid = ssid.substring(1, ssid.length() - 1);
            }
            menu.add(0, i, 0, ssid);
        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
            enableWifi(WifiStateControlService.ACTION_WIFI_ENABLE);
        }
        // 選択されたAPに接続
        WifiConfiguration config = mNetworkList.get(item.getItemId());
        mWifiManager.enableNetwork(config.networkId, true);
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     *
     * @param enable true:有効化/無効化
     */
    private void enableWifi(final String action) {
        // 状態が変わるまでボタンを無効化
        mWifiToggle.setEnabled(false);
        mWifiReenable.setEnabled(false);

        new AsyncTask<Object, Object, Boolean>() {
            @Override
            protected Boolean doInBackground(Object... params) {
                try {   // UIを優先し、ちょっと待つ
                    Thread.sleep(500);
                } catch (InterruptedException e) {}
                // これがあるとIS01で固まる
                //if (!enable) mWifiManager.disconnect();
                WifiStateControlService.startSerivce(WifiStateActivity.this, action);
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
                if (mNetworkStateInfo == null) {
                    mNetworkStateInfo = new NetworkStateInfo(WifiStateActivity.this);
                }
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
        String detail = mNetworkStateInfo.getDetail();
        String ip = mNetworkStateInfo.getLocalIpAddress();
        if (ip != null) {
            detail += " (" + ip + ")";
        }
        mNetworkStateText.setText(detail);
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
