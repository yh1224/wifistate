package net.orleaf.android.wifistate.core;

import android.content.Context;

public class WifiState {
    public static final String TAG = "WifiState";
    public static final String MARKET_URL = "market://details?id=net.orleaf.android.wifistate.plus";

    private static final String PACKAGE_NAME_LITE = "net.orleaf.android.wifistate";

    public static final boolean DEBUG = false;

    /**
     * 無料版チェック
     *
     * @param ctx Context
     */
    public static boolean isLiteVersion(Context ctx) {
        return ctx.getPackageName().equals(PACKAGE_NAME_LITE);
    }

}
