package net.assemble.wifistate;

import android.app.Activity;
import android.os.Bundle;

public class WifiStateActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WifiStateReceiver.testNotificationIcon(this);
        finish();
    }
}
