package com.yan.pullrefreshlayout;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

/**
 * Created by yan on 2017/4/11.
 */
public class PullRefreshView extends FrameLayout implements PullRefreshLayout.OnPullListener {
    private static final String TAG = "PullView";

    public PullRefreshView(Context context) {
        super(context);
        addView(LayoutInflater.from(getContext()).inflate(contentView(), this, false));
        initView();
    }

    protected void initView() {
    }

    protected int contentView() {
        return -1;
    }

    public PullRefreshView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PullRefreshView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onPullChange(float percent) {
        Log.e(TAG, "onPullChange: " + percent);
    }

    @Override
    public void onPullReset() {
        Log.e(TAG, "onPullReset: ");
    }

    @Override
    public void onPullHoldTrigger() {
        Log.e(TAG, "onPullHoldTrigger: ");
    }

    @Override
    public void onPullHoldUnTrigger() {
        Log.e(TAG, "onPullHoldUnTrigger: ");
    }

    @Override
    public void onPullHolding() {
        Log.e(TAG, "onPullHolding: ");
    }

    @Override
    public void onPullFinish() {
        Log.e(TAG, "onPullFinish: ");
    }

}
