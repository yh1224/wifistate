package net.orleaf.android.wifistate.core;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import net.orleaf.android.wifistate.BuildConfig;
import net.orleaf.android.wifistate.R;
import net.orleaf.android.wifistate.core.preferences.WifiStatePreferences;

/**
 * ネットワーク接続状態情報
 */
public class NetworkStateInfo {
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

    private final Context mCtx;
    private String mNetworkName;
    private States mState = States.STATE_DISABLED;
    private String mStateDetail = null;

    // Wi-Fi state
    private int mWifiState = 0;
    private WifiInfo mWifiInfo = null;
    private boolean mSupplicantConnected = false;
    private SupplicantState mSupplicantState = null;
    private NetworkInfo mWifiNetworkInfo = null;

    // Mobile network state
    private int mDataConnectionState;
    private NetworkInfo mDataNetworkInfo = null;

    /**
     * Constructor
     *
     * @param ctx Context
     */
    public NetworkStateInfo(Context ctx) {
        mCtx = ctx;
        mState = States.STATE_DISABLED;
        mNetworkName = null;
    }

    /**
     * 状態を更新する
     *
     * @return true:変更あり false:変更なし
     */
    public boolean update() {
        WifiManager wm = (WifiManager) mCtx.getSystemService(Context.WIFI_SERVICE);
        ConnectivityManager cm = (ConnectivityManager) mCtx.getSystemService(Context.CONNECTIVITY_SERVICE);
        TelephonyManager tm = (TelephonyManager) mCtx.getSystemService(Context.TELEPHONY_SERVICE);
        Resources res = mCtx.getResources();

        States newState = mState;
        String newStateDetail = mStateDetail;

        // Wi-Fiの状態を取得
        mWifiState = wm.getWifiState();
        mWifiInfo = wm.getConnectionInfo();
        mSupplicantState = mWifiInfo.getSupplicantState();
        if (mSupplicantState != SupplicantState.DISCONNECTED) {
            mSupplicantConnected = true;
        }
        mWifiNetworkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        // モバイルネットワークの状態を取得
        mDataConnectionState = tm.getDataState();
        mDataNetworkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if (mWifiState == WifiManager.WIFI_STATE_DISABLING ||
                mWifiState == WifiManager.WIFI_STATE_DISABLED) {
            // clear wifi state
            //wifiNetworkInfo = null;
            //supplicantConnected = false;
            //supplicantState = null;
            newState = States.STATE_DISABLED;
            newStateDetail = res.getString(R.string.state_unavailable);
        } else if (mWifiState == WifiManager.WIFI_STATE_ENABLING) {
            // -> enabled
            newState = States.STATE_WIFI_ENABLING;
            newStateDetail = res.getString(R.string.state_enabling);
        } else if (mWifiState == WifiManager.WIFI_STATE_ENABLED) {
            // enabling -> enabled
            if (mState.compareTo(States.STATE_WIFI_ENABLED) < 0) {
                newState = States.STATE_WIFI_ENABLED;
                newStateDetail = res.getString(R.string.state_enabled);
            }

            if (mWifiNetworkInfo != null && mWifiNetworkInfo.isAvailable() &&
                    mWifiNetworkInfo.getState() == NetworkInfo.State.CONNECTING &&
                    mWifiNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                newState = States.STATE_WIFI_OBTAINING_IPADDR;
                newStateDetail = res.getString(R.string.state_obtaining_ipaddr);
            } else if (mWifiNetworkInfo != null && mWifiNetworkInfo.isAvailable() &&
                    mWifiNetworkInfo.getState() == NetworkInfo.State.CONNECTED &&
                    mWifiNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                newState = States.STATE_WIFI_CONNECTED;
                newStateDetail = res.getString(R.string.state_connected);
            } else if (mSupplicantConnected && mSupplicantState != null) {
                if (mSupplicantState == SupplicantState.SCANNING) {
                    newState = States.STATE_WIFI_SCANNING;
                    newStateDetail = res.getString(R.string.state_scanning);
                } else if (mSupplicantState == SupplicantState.ASSOCIATING) {
                    newState = States.STATE_WIFI_CONNECTING;
                    newStateDetail = res.getString(R.string.state_associating);
                } else if (mSupplicantState == SupplicantState.ASSOCIATED) {
                    newState = States.STATE_WIFI_CONNECTING;
                    newStateDetail = res.getString(R.string.state_associated);
                } else if (mSupplicantState == SupplicantState.FOUR_WAY_HANDSHAKE ||
                           mSupplicantState == SupplicantState.GROUP_HANDSHAKE) {
                    newState = States.STATE_WIFI_CONNECTING;
                    newStateDetail = res.getString(R.string.state_handshaking);
                } else if (mSupplicantState == SupplicantState.COMPLETED) {
                    newState = States.STATE_WIFI_COMPLETED;
                    newStateDetail = res.getString(R.string.state_handshake_completed);
                } else if (mSupplicantState == SupplicantState.DISCONNECTED) {
                    newState = States.STATE_WIFI_SCANNING;
                    newStateDetail = res.getString(R.string.state_disconnected);
                }
            }
        }

        // モバイルネットワーク状態取得
        if (WifiStatePreferences.getShowDataNetwork(mCtx)) {
            // ・Wi-Fi無効時、または
            // ・スキャン中消去設定が有効でスキャン中
            if (newState == States.STATE_DISABLED ||
                    (newState == States.STATE_WIFI_SCANNING && WifiStatePreferences.getClearOnScanning(mCtx))) {
                if (mDataConnectionState == TelephonyManager.DATA_CONNECTING) {
                    newState = States.STATE_MOBILE_CONNECTING;
                    newStateDetail = res.getString(R.string.state_mobile_connecting);
                } else if (mDataConnectionState == TelephonyManager.DATA_CONNECTED) {
                    newState = States.STATE_MOBILE_CONNECTED;
                    newStateDetail = res.getString(R.string.state_mobile_connected);
                } else {
                    newState = States.STATE_DISABLED;
                    newStateDetail = res.getString(R.string.state_unavailable);
                }
            }
        }

        if (newState == null) {
            // no change
            if (BuildConfig.DEBUG) Log.d(WifiState.TAG, "State not recognized.");
            return false;
        }
        if (newState == mState && mStateDetail != null && newStateDetail.equals(mStateDetail)) {
            // 状態変更なし
            return false;
        }

        if (BuildConfig.DEBUG) Log.d(WifiState.TAG, "=>[" + newState + "] " + newStateDetail);
        mState = newState;
        mStateDetail = newStateDetail;

        // ネットワーク名を取得
        mNetworkName = null;
        if (newState.compareTo(States.STATE_WIFI_SCANNING) > 0 && /*スキャン完了済*/
                newState.compareTo(States.STATE_WIFI_CONNECTED) <= 0 &&
                mWifiInfo != null) {
            // SSID
            mNetworkName = mWifiInfo.getSSID();
        }
        else if (newState.compareTo(States.STATE_MOBILE_CONNECTING) >= 0) {
            // APN
            mNetworkName = mDataNetworkInfo.getExtraInfo();
        }

        return true;
    }

    /**
     * 消去可能な状態かどうか
     *
     * @return true:消去可能
     */
    public boolean isClearableState() {
        if (WifiStatePreferences.getClearOnDisabled(mCtx) && mState == States.STATE_DISABLED) {
            // ネットワーク無効時は表示しない
            return true;
        }
        if (WifiStatePreferences.getClearOnScanning(mCtx) && isScanning()) {
            // スキャン中は表示しない
            return true;
        }
        //noinspection RedundantIfStatement
        if (WifiStatePreferences.getClearOnConnected(mCtx) && isConnected()) {
            // 接続完了時は表示しない
            return true;
        }
        return false;
    }

    /**
     * アイコンを取得
     *
     * @return アイコンのリソースID
     */
    public int getIcon() {
        if (mState == States.STATE_DISABLED) {
            return R.drawable.state_0;
        } else if (mState == States.STATE_WIFI_1) {
            return R.drawable.state_w1;
        } else if (mState == States.STATE_WIFI_ENABLING) {
            return R.drawable.state_w2;
        } else if (mState == States.STATE_WIFI_ENABLED) {
            return R.drawable.state_w3;
        } else if (mState == States.STATE_WIFI_SCANNING) {
            return R.drawable.state_w4;
        } else if (mState == States.STATE_WIFI_CONNECTING) {
            return R.drawable.state_w5;
        } else if (mState == States.STATE_WIFI_COMPLETED) {
            return R.drawable.state_w6;
        } else if (mState == States.STATE_WIFI_OBTAINING_IPADDR) {
            return R.drawable.state_w7;
        } else if (mState == States.STATE_WIFI_CONNECTED) {
            return R.drawable.state_w8;
        } else if (mState == States.STATE_MOBILE_CONNECTING) {
            return R.drawable.state_m4;
        } else if (mState == States.STATE_MOBILE_CONNECTED) {
            return R.drawable.state_m8;
        }
        return 0;
    }

    /**
     * ネットワークの状態を取得
     */
    public States getState() {
        return mState;
    }

    /**
     * スキャン中かどうか
     */
    public boolean isScanning() {
        return (mState.equals(States.STATE_WIFI_SCANNING));
    }

    /**
     * ネットワークに接続中かどうか
     */
    public boolean isConnected() {
        return (mState.equals(States.STATE_MOBILE_CONNECTED) || mState.equals(States.STATE_WIFI_CONNECTED));
    }

    /**
     * Wi-Fiに接続中かどうか
     */
    public boolean isWifiConnected() {
        return (mState.equals(States.STATE_WIFI_CONNECTED));
    }

    /**
     * ネットワークタイプ
     */
    public String getNetworkType() {
        if (mState.compareTo(States.STATE_MOBILE_CONNECTING) >= 0) {
            return "Mobile Data";
        } else {
            return "Wi-Fi";
        }
    }

    /**
     * ネットワーク名
     */
    public String getNetworkName() {
        return mNetworkName;
    }

    /**
     * ネットワークの状態を取得
     */
    public String getDetail() {
        String detail = mStateDetail;
        if (mState == States.STATE_WIFI_CONNECTED || mState == States.STATE_MOBILE_CONNECTED) {
            String ip = getMyIpAddress();
            if (ip != null) {
                detail = "IP:" + ip;
            }
        }
        return detail;
    }

    /**
     * IPアドレスを取得
     */
    public String getMyIpAddress() {
        WifiManager wm = (WifiManager) mCtx.getSystemService(Context.WIFI_SERVICE);

        if (mState.equals(States.STATE_WIFI_CONNECTED)) {
            // Wi-Fi情報から取得
            int address = mWifiInfo.getIpAddress();
            if (address != 0) {
                return int2IpAddress(address);
            }

            // DHCP情報から取得
            address = wm.getDhcpInfo().ipAddress;
            if (address != 0) {
                return int2IpAddress(address);
            }
        }

        // ネットワークインタフェースからいずれかのグローバルアドレスを取得
        Enumeration<NetworkInterface> interfaces;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                Enumeration<InetAddress> addresses = network.getInetAddresses();

                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!(address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isAnyLocalAddress())) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * ゲートウェイアドレスを取得
     */
    public String getGatewayIpAddress() {
        WifiManager wm = (WifiManager) mCtx.getSystemService(Context.WIFI_SERVICE);

        if (mState.equals(States.STATE_WIFI_CONNECTED)) {
            DhcpInfo dhcpInfo = wm.getDhcpInfo();
            if (dhcpInfo.gateway != 0) {
                return int2IpAddress(dhcpInfo.gateway);
            }
        }
        return null;
    }

    /**
     * intをIPアドレス文字列に変換
     */
    private String int2IpAddress(int ip) {
        return
            ( ip        & 0xff) + "." +
            ((ip >>  8) & 0xff) + "." +
            ((ip >> 16) & 0xff) + "." +
            ((ip >> 24) & 0xff);
    }

}
