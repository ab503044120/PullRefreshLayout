package com.yan.refreshloadlayouttest;

import android.content.Context;
import android.text.TextUtils;
import android.widget.TextView;

import com.wang.avi.AVLoadingIndicatorView;
import com.yan.pullrefreshlayout.PullRefreshView;

/**
 * Created by yan on 2017/7/4.
 */

public class HeaderOrFooter extends PullRefreshView {
    private TextView tv;
    private AVLoadingIndicatorView loadingView;
    private String animationName;

    private boolean isStateFinish;

    public HeaderOrFooter(Context context, String animationName) {
        super(context);
        this.animationName = animationName;
        loadingView.setIndicator(animationName);
    }

    @Override
    protected int contentView() {
        return R.layout.refresh_view;
    }

    @Override
    protected void initView() {
        tv = (TextView) findViewById(R.id.title);
        loadingView = (AVLoadingIndicatorView) findViewById(R.id.loading_view);
        if (!TextUtils.isEmpty(animationName)) {
            loadingView.setIndicator(animationName);
        }
    }


    @Override
    public void onPullChange(float percent) {
        super.onPullChange(percent);
        if (isStateFinish) return;
        percent = Math.abs(percent);
        if (percent > 0.2) {
            if (loadingView.getVisibility() != VISIBLE) {
                loadingView.smoothToShow();
            }
            if (percent < 1) {
                loadingView.setScaleX(percent);
                loadingView.setScaleY(percent);
            }
        } else if (percent <= 0.2 && loadingView.getVisibility() == VISIBLE) {
            loadingView.smoothToHide();
        } else if (loadingView.getScaleX() != 1) {
            loadingView.setScaleX(1f);
            loadingView.setScaleY(1f);
        }

    }

    @Override
    public void onPullHoldTrigger() {
        super.onPullHoldTrigger();
        tv.setText("release loading");
    }

    @Override
    public void onPullHoldUnTrigger() {
        super.onPullHoldUnTrigger();
        tv.setText("drag");
    }

    @Override
    public void onPullHolding() {
        super.onPullHolding();
        tv.setText("loading...");
    }

    @Override
    public void onPullFinish() {
        super.onPullFinish();
        tv.setText("loading finish");
        isStateFinish = true;
        loadingView.smoothToHide();
    }

    @Override
    public void onPullReset() {
        super.onPullReset();
        tv.setText("drag");
        isStateFinish = false;
    }
}