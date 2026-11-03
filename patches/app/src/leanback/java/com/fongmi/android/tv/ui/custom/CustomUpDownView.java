package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fongmi.android.tv.utils.KeyUtil;
import com.google.android.material.textview.MaterialTextView;

public class CustomUpDownView extends MaterialTextView {

    private UpListener upListener;
    private DownListener downListener;

    public CustomUpDownView(@NonNull Context context) {
        super(context);
    }

    public CustomUpDownView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setUpListener(UpListener upListener) {
        this.upListener = upListener;
    }

    public void setDownListener(DownListener downListener) {
        this.downListener = downListener;
    }

    private boolean hasEvent(KeyEvent event) {
        // 上下键的 DOWN 和 UP 都必须吞掉。原来只吞 DOWN，UP 落到父布局焦点系统：
        // 上键 UP 会把焦点移出倍速按钮，表现为「往下翻能减速、往上翻没反应」。
        return (upListener != null && KeyUtil.isUpKey(event)) || (downListener != null && KeyUtil.isDownKey(event));
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (hasEvent(event)) return onKeyDown(event);
        else return super.dispatchKeyEvent(event);
    }

    private boolean onKeyDown(KeyEvent event) {
        if (!KeyUtil.isActionDown(event)) return true;
        if (upListener != null && KeyUtil.isUpKey(event)) upListener.onUp();
        if (downListener != null && KeyUtil.isDownKey(event)) downListener.onDown();
        return true;
    }

    public interface UpListener {

        void onUp();
    }

    public interface DownListener {

        void onDown();
    }
}
