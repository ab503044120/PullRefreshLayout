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

    public HeaderOrFooter(Context context, String animationName) {
        super(context);
        this.animationName = animationName;
        loadingView.setIndicator(animationName);
    }

    @Override
    protected int contentView() {
        return R.layout.load_more_view;
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
//                Log.e(TAG, "onPullChange: refresh " + percent);
    }

    @Override
    public void onPullReset() {
        tv.setText("drag");
        loadingView.hide();
    }

    @Override
    public void onPullHoldTrigger() {
        tv.setText("release loading");
    }

    @Override
    public void onPullHoldUnTrigger() {
        tv.setText("drag");
    }

    @Override
    public void onPullHolding() {
        tv.setText("loading...");
        loadingView.smoothToShow();
    }

    @Override
    public void onPullFinish() {
        tv.setText("loading finish");
        loadingView.smoothToHide();
    }
}