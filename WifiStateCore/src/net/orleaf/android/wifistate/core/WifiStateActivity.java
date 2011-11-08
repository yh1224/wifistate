package net.orleaf.android.wifistate.core;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class WifiStateActivity extends Activity {
    private boolean launched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!launched) {
            startActivity(new Intent(this, WifiStateStatusActivity.class));
            launched = true;
        } else {
            finish();
        }
    }

}
