package net.orleaf.android.wifistate.core;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
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
    private final WifiManager mWifiManager;
    private final ConnectivityManager mConnectivityManager;
    private final TelephonyManager mTelephonyManager;

    private String mNetworkName;
    private States mState = States.STATE_DISABLED;
    private String mStateDetail = null;

    // Wi-Fi state
    private int mWifiState = 0;
    private WifiInfo mWifiInfo = null;
    private List<ScanResult> mScanResults = null;
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
        mWifiManager = (WifiManager) mCtx.getSystemService(Context.WIFI_SERVICE);
        mConnectivityManager = (ConnectivityManager) mCtx.getSystemService(Context.CONNECTIVITY_SERVICE);
        mTelephonyManager = (TelephonyManager) mCtx.getSystemService(Context.TELEPHONY_SERVICE);
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
        mWifiInfo = mWifiManager.getConnectionInfo();
        mScanResults = mWifiManager.getScanResults();
        mSupplicantState = mWifiInfo.getSupplicantState();
        if (mSupplicantState != SupplicantState.DISCONNECTED) {
            mSupplicantConnected = true;
        }
        mWifiNetworkInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        // モバイルネットワークの状態を取得
        mDataConnectionState = mTelephonyManager.getDataState();
        mDataNetworkInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

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
        if (WifiStatePreferences.getClearOnScanning(mCtx) && isWifiScanning()) {
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
     * ネットワークに接続済かどうか (Wi-Fiまたはモバイルネットワーク)
     */
    public boolean isConnected() {
        return (mState == States.STATE_MOBILE_CONNECTED || mState == States.STATE_WIFI_CONNECTED);
    }

    /**
     * Wi-Fiをスキャン中かどうか
     */
    public boolean isWifiScanning() {
        return (mState == States.STATE_WIFI_SCANNING);
    }

    /**
     * Wi-Fi接続先決定済かどうか
     */
    public boolean isWifiAPDecided() {
        return (mState.compareTo(States.STATE_WIFI_SCANNING) > 0 &&
                mState.compareTo(States.STATE_WIFI_CONNECTED) <= 0);
    }

    /**
     * Wi-Fiに接続済かどうか
     */
    public boolean isWifiConnected() {
        return (mState == States.STATE_WIFI_CONNECTED);
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
     * ネットワークの状態情報を取得
     *
     * @return 状態情報 (IP取得済みであればIPアドレス情報)
     */
    public String getStateDetail() {
        String detail = mStateDetail;
        if (isConnected()) {
            String ip = getMyIpAddress();
            if (ip != null) {
                detail = "IP:" + ip;
            }
        }
        return detail;
    }

    /**
     * ネットワークの追加情報を取得
     *
     * @return 追加情報 (null:未接続)
     */
    public String getExtraInfo() {
        String detail = null;
        ScanResult scanResult = getScanResult();
        if (isWifiAPDecided()) {
            // Signal Level
            int rssi = mWifiInfo.getRssi();
            detail = "Signal: " + WifiManager.calculateSignalLevel(rssi, 5) + "/4 (" + rssi + "dBm)";

            // Channel
            int freq = getWifiFrequency();
            if (freq < 0 && scanResult != null) {
                // WifiInfo から取得できない場合は ScanResult から取得
                freq = scanResult.frequency;
            }
            if (freq > 0) {
                int ch = convertFrequencyToChannel(freq);
                if (ch > 0) {
                    // ex) "36CH (5,180MHz)"
                    detail += "\nChannel: " + ch + "CH (" + NumberFormat.getNumberInstance().format(freq) + "MHz)";
                }
            }

            // Link Speed
            int speed = mWifiInfo.getLinkSpeed();
            if (speed > 0) {
                detail += "\nSpeed: " + speed + WifiInfo.LINK_SPEED_UNITS;
            }
        }
        return detail;
    }

    /**
     * 接続中の ScanResult を取得
     * @return ScanResult
     */
    private ScanResult getScanResult() {
        if (mScanResults != null) {
            for (ScanResult scanResult : mScanResults) {
                if (scanResult.BSSID.equals(mWifiInfo.getBSSID())) {
                    return scanResult;
                }
            }
        }
        return null;
    }

    /**
     * WifiInfo から周波数を取得 (requires API Level 21)
     *
     * @return 周波数 (-1:取得不可)
     */
    public int getWifiFrequency() {
        try {
            Method method = WifiInfo.class.getMethod("getFrequency");
            return (Integer) method.invoke(mWifiInfo);
        } catch (NoSuchMethodException ignored) {
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * ScanResult から値を取得
     *
     * @param scanResult ScanResult
     * @param fieldName field name
     * @return field value (-1:N/A)
     */
    private int getScanResultInt(ScanResult scanResult, String fieldName) {
        try {
            Field field = ScanResult.class.getField(fieldName);
            return (Integer) field.get(scanResult);
        } catch (NoSuchFieldException ignored) {
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * IPアドレスを取得
     */
    public String getMyIpAddress() {
        if (mState.equals(States.STATE_WIFI_CONNECTED)) {
            // Wi-Fi情報から取得
            int address = mWifiInfo.getIpAddress();
            if (address != 0) {
                return int2IpAddress(address);
            }

            // DHCP情報から取得
            address = mWifiManager.getDhcpInfo().ipAddress;
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

    /**
     * 周波数からチャネル番号に変換
     *
     * @param freq 周波数(MHz)
     * @return チャネル番号 (-1:変換不可)
     */
    public static int convertFrequencyToChannel(int freq) {
        if (freq >= 2412 && freq <= 2484) {
            return (freq - 2412) / 5 + 1;
        } else if (freq >= 5170 && freq <= 5825) {
            return (freq - 5170) / 5 + 34;
        } else {
            return -1;
        }
    }
}
