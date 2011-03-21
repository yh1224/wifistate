package net.orleaf.android.wifistate;

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

    private Context mCtx;
    private String mNetworkName;
    private States mState = States.STATE_DISABLED;
    private String mStateDetail = null;

    // Managers
    private WifiManager mWifiManager;
    private ConnectivityManager mConnManager;
    private TelephonyManager mTelManager;

    // Wi-Fi state
    private int mWifiState = 0;
    private boolean mSupplicantConnected = false;
    private SupplicantState mSupplicantState = null;
    private NetworkInfo mWifiNetworkInfo = null;

    // Mobile network state
    private int mDataConnectionState;
    private NetworkInfo mDataNetworkInfo = null;

    /**
     * Constructor
     *
     * @param ctx
     */
    public NetworkStateInfo(Context ctx) {
        mCtx = ctx;
        mWifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        mConnManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        mTelManager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        mState = States.STATE_DISABLED;
        mNetworkName = null;
    }

    /**
     * 状態を更新する
     * 
     * @return true:変更あり false:変更なし
     */
    public boolean update() {
        Resources res = mCtx.getResources();
        States newState = mState;
        String newStateDetail = mStateDetail;

        // Wi-Fiの状態を取得
        mWifiState = mWifiManager.getWifiState();
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        mSupplicantState = wifiInfo.getSupplicantState();
        if (mSupplicantState != SupplicantState.DISCONNECTED) {
            mSupplicantConnected = true;
        }
        mWifiNetworkInfo = mConnManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        // モバイルネットワークの状態を取得
        mDataConnectionState = mTelManager.getDataState();
        mDataNetworkInfo = mConnManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if (mWifiState == WifiManager.WIFI_STATE_DISABLING ||
                mWifiState == WifiManager.WIFI_STATE_DISABLED) {
            // clear wifi state
            //wifiNetworkInfo = null;
            //supplicantConnected = false;
            //supplicantState = null;
            if (WifiStatePreferences.getShowDataNetwork(mCtx)) {
                if (mDataConnectionState == TelephonyManager.DATA_CONNECTING) {
                    newState = States.STATE_MOBILE_CONNECTING;
                    newStateDetail = res.getString(R.string.mobile_connecting);
                } else if (mDataConnectionState == TelephonyManager.DATA_CONNECTED) {
                    newState = States.STATE_MOBILE_CONNECTED;
                    newStateDetail = res.getString(R.string.mobile_connected);
                } else {
                    newState = States.STATE_DISABLED;
                    newStateDetail = res.getString(R.string.unavailable);
                }
            } else {
                newState = States.STATE_DISABLED;
                newStateDetail = res.getString(R.string.unavailable);
            }
        } else if (mWifiState == WifiManager.WIFI_STATE_ENABLING) {
            // -> enabled
            newState = States.STATE_WIFI_ENABLING;
            newStateDetail = res.getString(R.string.enabling);
        } else if (mWifiState == WifiManager.WIFI_STATE_ENABLED) {
            // enabling -> enabled
            if (mState.compareTo(States.STATE_WIFI_ENABLED) < 0) {
                newState = States.STATE_WIFI_ENABLED;
                newStateDetail = res.getString(R.string.enabled);
            }

            if (mWifiNetworkInfo != null && mWifiNetworkInfo.isAvailable() &&
                    mWifiNetworkInfo.getState() == NetworkInfo.State.CONNECTING &&
                    mWifiNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                newState = States.STATE_WIFI_OBTAINING_IPADDR;
                newStateDetail = res.getString(R.string.obtaining_ipaddr);
            } else if (mWifiNetworkInfo != null && mWifiNetworkInfo.isAvailable() &&
                    mWifiNetworkInfo.getState() == NetworkInfo.State.CONNECTED &&
                    mWifiNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                newState = States.STATE_WIFI_CONNECTED;
                newStateDetail = res.getString(R.string.connected);
            } else if (mSupplicantConnected && mSupplicantState != null) {
                if (mSupplicantState == SupplicantState.SCANNING) {
                    newState = States.STATE_WIFI_SCANNING;
                    newStateDetail = res.getString(R.string.scanning);
                } else if (mSupplicantState == SupplicantState.ASSOCIATING) {
                    newState = States.STATE_WIFI_CONNECTING;
                    newStateDetail = res.getString(R.string.associating);;
                } else if (mSupplicantState == SupplicantState.ASSOCIATED) {
                    newState = States.STATE_WIFI_CONNECTING;
                    newStateDetail = res.getString(R.string.associated);
                } else if (mSupplicantState == SupplicantState.FOUR_WAY_HANDSHAKE ||
                           mSupplicantState == SupplicantState.GROUP_HANDSHAKE) {
                    newState = States.STATE_WIFI_CONNECTING;
                    newStateDetail = res.getString(R.string.handshaking);
                } else if (mSupplicantState == SupplicantState.COMPLETED) {
                    newState = States.STATE_WIFI_COMPLETED;
                    newStateDetail = res.getString(R.string.handshake_completed);
                } else if (mSupplicantState == SupplicantState.DISCONNECTED) {
                    newState = States.STATE_WIFI_SCANNING;
                    newStateDetail = res.getString(R.string.disconnected);
                }
            }
        }

        if (newState == null) {
            // no change
            if (WifiState.DEBUG) Log.d(WifiState.TAG, "State not recognized.");
            return false;
        }
        if (newState == mState && mStateDetail != null && newStateDetail.equals(mStateDetail)) {
            // 状態変更なし
            return false;
        }

        if (WifiState.DEBUG) Log.d(WifiState.TAG, "=>[" + newState + "] " + newStateDetail);
        mState = newState;
        mStateDetail = newStateDetail;

        // ネットワーク名を取得
        mNetworkName = null;
        if (newState.compareTo(States.STATE_WIFI_SCANNING) > 0 && /*スキャン完了済*/
                newState.compareTo(States.STATE_WIFI_CONNECTED) <= 0 &&
                wifiInfo != null) {
            // SSID
            mNetworkName = wifiInfo.getSSID();
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
     * @param ctx コンテキスト
     * @param states 状態
     * @return true:消去可能
     */
    public boolean isClearableState() {
        if (WifiStatePreferences.getClearOnDisabled(mCtx) && mState == States.STATE_DISABLED) {
            return true;
        }
        if (WifiStatePreferences.getClearOnConnected(mCtx) &&
                    (mState == States.STATE_WIFI_CONNECTED || mState == States.STATE_MOBILE_CONNECTED)) {
            return true;
        }
        return false;
    }

    /**
     * アイコンを取得
     *
     * @param state 状態
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
     * ネットワーク名
     */
    public String getNetworkName() {
        return mNetworkName;
    }

    /**
     * ネットワークの状態を取得
     */
    public String getDetail() {
        return mStateDetail;
    }

    /**
     * IPアドレスを取得
     */
    public String getLocalIpAddress() {
        if (mState.equals(States.STATE_WIFI_CONNECTED)) {
            DhcpInfo dhcpInfo = mWifiManager.getDhcpInfo();
            if (dhcpInfo.ipAddress != 0) {
                return int2IpAddress(dhcpInfo.ipAddress);
            }
        }
        return null;
    }

    /**
     * ゲートウェイアドレスを取得
     */
    public String getGatewayIpAddress() {
        if (mState.equals(States.STATE_WIFI_CONNECTED)) {
            DhcpInfo dhcpInfo = mWifiManager.getDhcpInfo();
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
