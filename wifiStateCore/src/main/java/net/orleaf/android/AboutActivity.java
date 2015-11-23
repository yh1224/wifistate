package net.orleaf.android;

import java.io.IOException;

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

import net.orleaf.android.wifistate.core.R;

public class AboutActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_LEFT_ICON);

        setContentView(R.layout.about);

        Intent intent = getIntent();
        String title = intent.getStringExtra("title");

        // タイトル
        if (title == null) {
            title = getResources().getString(R.string.app_name);
        }
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            title += " ver." + pi.versionName;
        } catch (NameNotFoundException e) {
            throw new AssertionError(e);
        }
        setTitle(title);

        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
                R.drawable.icon);

        String bodyText = intent.getStringExtra("body");
        String bodyAsset = intent.getStringExtra("body_asset");

        TextView text = (TextView) findViewById(R.id.text);
        if (bodyText != null) {
            text.setText(bodyText);
        } else if (bodyAsset != null) {
            // assetによる指定
            AssetsReader ar = new AssetsReader(this);
            try {
                String str = ar.getText(bodyAsset);
                text.setText(str);
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        }

        // OK
        Button btn_ok = (Button) findViewById(R.id.ok);
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }

        });

        // Feedback
        Button btn_feedback = (Button) findViewById(R.id.support);
        btn_feedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                        getString(R.string.support_url)));
                startActivity(intent);
            }
        });
    }
}
