package com.yan.refreshloadlayouttest;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.wang.avi.AVLoadingIndicatorView;
import com.yan.refreshloadlayouttest.widget.PullRefreshView;

/**
 * Created by yan on 2017/7/4.
 */

public class HeaderOrFooter extends PullRefreshView {
    private TextView tv;
    private AVLoadingIndicatorView loadingView;
    private String animationName;

    private boolean isStateFinish;
    private int color;
    private boolean isHolding;

    public HeaderOrFooter(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(getContext()).inflate(contentView(), this, true);
        initView();
        color = ContextCompat.getColor(context, R.color.colorPrimary);
        this.animationName = "LineScaleIndicator";
        loadingView.setIndicator(animationName);
        loadingView.setIndicatorColor(color);
    }

    public HeaderOrFooter(Context context) {
        super(context);
        initView();
        color = ContextCompat.getColor(context, R.color.colorPrimary);
        this.animationName = "LineScaleIndicator";
        loadingView.setIndicator(animationName);
        loadingView.setIndicatorColor(color);
    }

    public HeaderOrFooter(Context context, String animationName) {
        super(context);
        color = ContextCompat.getColor(context, R.color.colorPrimary);
        this.animationName = animationName;
        loadingView.setIndicator(animationName);
        loadingView.setIndicatorColor(color);
    }

    public HeaderOrFooter(Context context, String animationName, int color) {
        this(context, animationName, color, true);
    }

    public HeaderOrFooter(Context context, String animationName, int color, boolean withBg) {
        super(context);
        this.color = color;
        loadingView.setIndicator(animationName);
        loadingView.setIndicatorColor(color);
        tv.setTextColor(color);
        if (withBg) {
            setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        }
    }

    @Override
    protected int contentView() {
        return R.layout.refresh_view;
    }

    @Override
    protected void initView() {
        tv = (TextView) findViewById(R.id.title);
        loadingView = (AVLoadingIndicatorView) findViewById(R.id.loading_view);
    }

    @Override
    public void onPullChange(float percent) {
        super.onPullChange(percent);
        if (isStateFinish || isHolding) return;
        percent = Math.abs(percent);
        if (percent > 0.2 && percent < 1) {
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
        isHolding = true;
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
        isHolding = false;

    }
}