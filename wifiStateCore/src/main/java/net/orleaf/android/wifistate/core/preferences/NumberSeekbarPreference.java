package net.orleaf.android.wifistate.core.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import net.orleaf.android.wifistate.core.R;

/**
 * シークバーと選択中の値を表示するプリファレンス
 */
public class NumberSeekbarPreference extends DialogPreference {
    private int mValue;         // 選択中の値
    private int mMinValue;      // 最小値
    private int mMaxValue;      // 最大値
    private String mUnit;       // 表示単位
    private String mZero;       // 0のときの表示

    public NumberSeekbarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.number_seekbar_dialog);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        mMinValue = attrs.getAttributeIntValue(null, "minValue", -1);
        mMaxValue = attrs.getAttributeIntValue(null, "maxValue", -1);
        mUnit = context.getResources().getString(attrs.getAttributeResourceValue(null, "unit", -1));
        int zero = attrs.getAttributeResourceValue(null, "zero", -1);
        if (zero != -1) {
            mZero = context.getResources().getString(zero);
        }
    }

    public void setValue(int value) {
        mValue = value;
        persistInt(value);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedInt(mValue) : (Integer) defaultValue);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        final TextView textView = getTextView(view);
        final SeekBar seekBar = getSeekBar(view);

        seekBar.setMax(mMaxValue);
        seekBar.setProgress(mValue);
        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mValue = seekBar.getProgress();
                if (mValue < mMinValue) {
                    mValue = mMinValue;
                    seekBar.setProgress(mValue);
                }
                if (mValue == 0 && mZero != null) {
                    textView.setText(mZero);
                } else {
                    textView.setText(mValue + mUnit);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        if (mValue == 0 && mZero != null) {
            textView.setText(mZero);
        } else {
            textView.setText(mValue + mUnit);
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            setValue(mValue);
        }
    }

    public int getValue() {
        return mValue;
    }

    protected static TextView getTextView(View dialogView) {
        return (TextView) dialogView.findViewById(R.id.number);
    }

    protected static SeekBar getSeekBar(View dialogView) {
        return (SeekBar) dialogView.findViewById(R.id.seekbar);
    }

}
