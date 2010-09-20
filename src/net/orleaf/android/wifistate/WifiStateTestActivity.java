package net.orleaf.android.wifistate;

import android.app.Activity;
import android.os.Bundle;

public class WifiStateTestActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WifiStateReceiver.testNotificationIcon(this);
        finish();
    }
}
