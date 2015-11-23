package net.orleaf.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

import android.content.Context;
import android.content.res.AssetManager;

public class AssetsReader {
    private Context mCtx;

    public AssetsReader(Context ctx) {
        mCtx = ctx;
    }

    /**
     * テキストファイルから文字列を読み込む
     *
     * @param filename ファイル名
     * @return 文字列
     */
    public String getText(String filename) throws IOException {
        // テキスト
        AssetManager as = mCtx.getAssets();
        InputStream fin;
        try {
            fin = as.open(filename + "." + Locale.getDefault().getLanguage());
        } catch (IOException e) {
            fin = as.open(filename);
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(fin));
        StringBuilder str = new StringBuilder();
        String s;
        while ((s = in.readLine()) != null) {
            str.append(s).append("\n");
        }
        in.close();
        return str.toString();
    }

}
