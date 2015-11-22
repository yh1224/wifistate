package net.orleaf.android.wifistate.core;

import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import net.orleaf.android.AboutActivity;
import net.orleaf.android.wifistate.core.preferences.WifiStatePreferencesActivity;

/**
 * メイン画面
 */
public class WifiStateStatusActivity extends Activity {

    private BroadcastReceiver mConnectivityReceiver = null;

    private NetworkStateInfo mNetworkStateInfo = null;
    private List<WifiConfiguration> mNetworkList = null;
    private Handler mHandler = new Handler();

    // Views
    private ToggleButton mWifiToggle;
    private Button mWifiSettings;
    private Button mWifiReenable;
    private LinearLayout mNetworkLayout;
    private TextView mNetworkNameText;
    private TextView mNetworkStateText;
    private MenuItem mRouterMenu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_LEFT_ICON);

        setContentView(R.layout.main);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
                R.drawable.icon);

        mNetworkLayout = (LinearLayout) findViewById(R.id.network);
        registerForContextMenu(mNetworkLayout);

        mNetworkNameText = (TextView) findViewById(R.id.network_name);
        mNetworkStateText = (TextView) findViewById(R.id.network_status);

        // Wi-Fi 設定ボタン
        mWifiSettings = (Button) findViewById(R.id.wifi_settings);
        mWifiSettings.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
            }
        });

        // Wi-Fi ON/OFFボタン
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
        mWifiToggle.setEnabled(false);

        // 再接続ボタン
        mWifiReenable = (Button) findViewById(R.id.wifi_reenable);
        mWifiReenable.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                enableWifi(WifiStateControlService.ACTION_WIFI_REENABLE);
            }
        });
        mWifiReenable.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // 長押しの場合はダイアログを閉じる。
                enableWifi(WifiStateControlService.ACTION_WIFI_REENABLE);
                finish();
                return true;
            }
        });
        mWifiReenable.setEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 少し待ってから情報を取得
        mHandler.postDelayed(mStartUpdate, 500);
    }

    /**
     * ネットワーク接続状態取得開始
     */
    private Runnable mStartUpdate = new Runnable() {
        @Override
        public void run() {
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

            // 初回実行
            updateStatus();
        }
    };

    @Override
    protected void onPause() {
        super.onPause();

        // ネットワーク接続状態監視停止
        if (mConnectivityReceiver != null) {
            unregisterReceiver(mConnectivityReceiver);
            mConnectivityReceiver = null;
        }
        mHandler.removeCallbacks(mStartUpdate);
    }

    /**
     * オプションメニューの生成
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mRouterMenu = menu.findItem(R.id.menu_router);
        if (mNetworkStateInfo.getGatewayIpAddress() != null) {
            mRouterMenu.setVisible(true);
        } else {
            mRouterMenu.setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * オプションメニュー選択時の処理
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_config) {
            // 設定
            Intent intent = new Intent().setClass(this, WifiStatePreferencesActivity.class);
            startActivity(intent);
        } else if (itemId == R.id.menu_router) {
            // ルータ設定
            String gatewayAddr = mNetworkStateInfo.getGatewayIpAddress();
            if (gatewayAddr != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + gatewayAddr));
                startActivity(intent);
            }
        } else if (itemId == R.id.menu_about) {
            // バージョン情報
            Intent intent = new Intent().setClass(this, AboutActivity.class);
            intent.putExtra("body_asset", "about.txt");
            startActivity(intent);
        }
        return true;
    };

    /**
     * コンテキストメニュー(接続先)の生成
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        menu.setHeaderTitle(R.string.connect_to);

        // SSID一覧
        if (mNetworkList == null) {
            mNetworkList = wm.getConfiguredNetworks();
            if (mNetworkList == null) { // 取得失敗
                return;
            }
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

    /**
     * コンテキストメニュー(接続先)選択時の処理
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        if (wm.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
            // Wi-Fi 無効時は有効化
            enableWifi(WifiStateControlService.ACTION_WIFI_ENABLE);
        }

        // 選択されたAPに接続
        WifiConfiguration config = mNetworkList.get(item.getItemId());
        wm.enableNetwork(config.networkId, true);

        return super.onContextItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Wi-Fi 有効化/無効化/再有効化
     *
     * @param action アクション
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
                WifiStateControlService.startService(WifiStateStatusActivity.this, action);
                return true;
            }
        }.execute();
    }

    /**
     * Wi-Fi状態情報を更新し、表示に反映する (非同期)
     */
    private void updateStatus() {
        new AsyncTask<Object, Object, Boolean>() {
            @Override
            protected Boolean doInBackground(Object... params) {
                if (mNetworkStateInfo == null) {
                    mNetworkStateInfo = new NetworkStateInfo(WifiStateStatusActivity.this);
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
        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        mNetworkNameText.setText(mNetworkStateInfo.getNetworkName());
        mNetworkStateText.setText(mNetworkStateInfo.getDetail());
        if (wm.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
            mWifiToggle.setChecked(false);
            mWifiToggle.setEnabled(true);
            mWifiReenable.setEnabled(false);
        } else if (wm.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            mWifiToggle.setChecked(true);
            mWifiToggle.setEnabled(true);
            mWifiReenable.setEnabled(true);
        }
    }

}
