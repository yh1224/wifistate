package net.orleaf.android.wifistate.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * サービス
 */
public class WifiStatePingService extends Service {
    public static final String EXTRA_TARGET = "target";

    private static final boolean TESTMODE = false;

    private static ComponentName mService;

    private boolean mReachable;
    private String mTarget;
    private Thread mThead = null;
    private boolean mRunning;
    private int mNumPing;
    private int mNumOk;
    private int mNumNg;

    @Override
    public void onCreate() {
        super.onCreate();
        start();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        String target = WifiStatePreferences.getPingTarget(this);
        mReachable = true;
        if (target == null || target.equals("")) {
            mTarget = intent.getStringExtra(EXTRA_TARGET);
        } else {
            mTarget = target;
        }
        start();
    }

    /**
     * 監視開始
     */
    private void start() {
        if (WifiState.DEBUG) Log.d(WifiState.TAG, "Target: " + mTarget);
        if (mTarget != null) {
            mNumPing = 0;
            mNumOk = 0;
            mNumNg = 0;
            startThread();
        } else {
            stopThread();
        }
    }

    /**
     * 監視スレッド開始
     */
    private void startThread() {
        mThead = new Thread() {
            @Override
            public void run() {
                if (WifiState.DEBUG) Log.d(WifiState.TAG, "Thread started.");
                while (mRunning) {
                    if (WifiState.DEBUG) Log.d(WifiState.TAG, "Pinging: " + mTarget);

                    boolean reachable = false;
                    int ntry = WifiStatePreferences.getPingRetry(WifiStatePingService.this) + 1;
                    for (int i = 0; i < ntry; i++) {
                        mNumPing++;
                        if (TESTMODE) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            reachable = !mReachable;
                        } else {
                            reachable = ping(mTarget, 1000);
                        }
                        if (reachable) {
                            mNumOk++;
                        } else {
                            mNumNg++;
                        }
                        if (reachable != mReachable) {
                            notifyReachability(reachable);
                        }
                        mReachable = reachable;
                        if (reachable) {
                            break;
                        }
                    }
                    try {
                        Thread.sleep(WifiStatePreferences.getPingInterval(WifiStatePingService.this) * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (WifiState.DEBUG) Log.d(WifiState.TAG, "Thread stoppped.");
            }

            /**
             * ping
             */
            private boolean ping(String target, int timeout) {
                boolean result = false;

                InetAddress inetAddress = null;
                try {
                    inetAddress = InetAddress.getByName(target);
                } catch (UnknownHostException e) {
                    Log.e(WifiState.TAG, "name resolution failed: " + target);
                    e.printStackTrace();
                }
                if (inetAddress != null) {
                    try {
                        String[] cmdLine = new String[] { "ping", "-c", "1", inetAddress.getHostAddress() };
                        Process process = Runtime.getRuntime().exec(cmdLine);
                        process.waitFor();
                        //String out = readAll(process.getInputStream());
                        //String err = readAll(process.getErrorStream());
                        process.destroy();
                        if (process.exitValue() == 0) {
                            if (WifiState.DEBUG) Log.d(WifiState.TAG, "ping success: " + inetAddress.getHostAddress());
                            result = true;
                        } else {
                            Log.e(WifiState.TAG, "ping failed: " + inetAddress.getHostAddress());
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return result;
            }

            /**
             * 結果を取得する
             */
            @SuppressWarnings("unused")
            private String readAll(InputStream stream) throws IOException {
                String line;
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream), 1024);
                StringBuffer msg = new StringBuffer();
                while ((line = reader.readLine()) != null) {
                    if (WifiState.DEBUG) Log.v(WifiState.TAG, " |" + line);
                    msg.append(line + "\n");
                }
                return msg.toString().trim();
            }
        };
        mRunning = true;
        mThead.start();
    }

    /**
     * 監視停止
     */
    private void stopThread() {
        if (mThead != null) {
            mRunning = false;
            mThead = null;
        }
    }

    /**
     * 到達性通知
     */
    private void notifyReachability(boolean reachable) {
        Intent intent = new Intent(this, WifiStateReceiver.class);
        intent.setAction(WifiStateReceiver.ACTION_REACHABILITY);
        intent.putExtra(WifiStateReceiver.EXTRA_REACHABLE, reachable);
        intent.putExtra(WifiStateReceiver.EXTRA_COUNT_OK, mNumOk);
        intent.putExtra(WifiStateReceiver.EXTRA_COUNT_NG, mNumNg);
        intent.putExtra(WifiStateReceiver.EXTRA_COUNT_TOTAL, mNumPing);
        sendBroadcast(intent);
    }

    public void onDestroy() {
        stopThread();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    /**
     * サービス開始
     */
    public static boolean startService(Context ctx, String target) {
        boolean result;

        Intent intent = new Intent(ctx, WifiStatePingService.class);
        intent.putExtra(EXTRA_TARGET, target);
        mService = ctx.startService(intent);
        if (mService == null) {
            Log.e(WifiState.TAG, "WifiStatePingService could not start!");
            result = false;
        } else {
            Log.d(WifiState.TAG, "Service started: " + mService);
            result = true;
        }
        return result;
    }

    /**
     * サービス停止
     */
    public static void stopService(Context ctx) {
        if (mService != null) {
            Intent i = new Intent();
            i.setComponent(mService);
            boolean res = ctx.stopService(i);
            if (res == false) {
                Log.e(WifiState.TAG, "WifiStatePingService could not stop!");
            } else {
                Log.d(WifiState.TAG, "Service stopped: " + mService);
                mService = null;
            }
        }
    }

}
