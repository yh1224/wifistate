package net.orleaf.android;

import java.io.IOException;

import net.orleaf.android.wifistate.core.R;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class AboutActivity extends Activity
{
    private String mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_LEFT_ICON);

        setContentView(R.layout.about);

        // タイトル
        mTitle = getResources().getString(R.string.app_name);
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            mTitle += " ver." + pi.versionName;
        } catch (NameNotFoundException e) {}
        setTitle(mTitle);

        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
                R.drawable.icon);

        // テキスト
        AssetsReader ar = new AssetsReader(this);
        try {
            String str = ar.getText("about.txt");
            TextView text = (TextView) findViewById(R.id.text);
            text.setText(str);
        } catch (IOException e) {}

        // OK
        Button btn_ok = (Button) findViewById(R.id.ok);
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }

        });

        // Feedback
        Button btn_feedback = (Button) findViewById(R.id.feedback);
        btn_feedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(
                        "mailto:" + getResources().getString(R.string.feedback_to)));
                intent.putExtra(Intent.EXTRA_SUBJECT, mTitle);
                startActivity(intent);
            }
        });
    }
}
