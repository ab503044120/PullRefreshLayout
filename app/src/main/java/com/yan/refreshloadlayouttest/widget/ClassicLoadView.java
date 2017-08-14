package com.yan.refreshloadlayouttest.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wang.avi.AVLoadingIndicatorView;
import com.yan.pullrefreshlayout.PullRefreshLayout;
import com.yan.refreshloadlayouttest.R;

/**
 * Created by yan on 2017/8/14.
 */

public class ClassicLoadView extends FrameLayout implements PullRefreshLayout.OnPullListener {
    private TextView tv;
    private AVLoadingIndicatorView loadingView;

    private boolean isStateFinish;
    private PullRefreshLayout refreshLayout;

    private ObjectAnimator objectAnimator;

    public ClassicLoadView(@NonNull Context context, final PullRefreshLayout refreshLayout) {
        super(context);
        this.refreshLayout = refreshLayout;

        setLayoutParams(new ViewGroup.LayoutParams(-1, -1));

        LayoutInflater.from(getContext()).inflate(R.layout.refresh_view, this, true);
        tv = (TextView) findViewById(R.id.title);
        tv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "you just touched me", Toast.LENGTH_SHORT).show();
            }
        });
        loadingView = (AVLoadingIndicatorView) findViewById(R.id.loading_view);

        loadingView.setIndicator("LineScaleIndicator");
        loadingView.setIndicatorColor(ContextCompat.getColor(context, R.color.colorPrimary));

        objectAnimator = ObjectAnimator.ofFloat(this, "y", 0, 0);
        objectAnimator.setDuration(300);
        objectAnimator.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                ClassicLoadView.this.refreshLayout.loadMoreComplete();
                ClassicLoadView.this.refreshLayout.setMoveWithFooter(true);
                refreshLayout.setDispatchPullTouchAble(true);
            }
        });
        setBackgroundColor(Color.WHITE);

    }

    public void startBackAnimation() {
        refreshLayout.setDispatchPullTouchAble(false);
        refreshLayout.setMoveWithFooter(false);
        refreshLayout.moveChildren(0);
        objectAnimator.setFloatValues(getY(), getY() + getMeasuredHeight());
        objectAnimator.start();
    }

    @Override
    public void onPullChange(float percent) {
        if (refreshLayout.isDragDown() || refreshLayout.isDragUp()) {
            return;
        }
        if (!refreshLayout.isTargetAbleScrollDown()) {
            refreshLayout.autoLoading();
        }
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
        loadingView.smoothToShow();
        tv.setText("loading...");
    }

    @Override
    public void onPullFinish() {
        tv.setText("loading finish");
        isStateFinish = true;
        loadingView.smoothToHide();
    }

    @Override
    public void onPullReset() {
        tv.setText("drag");
        isStateFinish = false;
    }
}
