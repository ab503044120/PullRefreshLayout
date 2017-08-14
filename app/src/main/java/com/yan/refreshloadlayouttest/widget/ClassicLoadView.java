package com.yan.refreshloadlayouttest.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
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

    private PullRefreshLayout refreshLayout;

    private ObjectAnimator objectAnimator;

    public ClassicLoadView(@NonNull Context context, final PullRefreshLayout refreshLayout) {
        super(context);
        this.refreshLayout = refreshLayout;

        // 设置 布局 为 match_parent
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
                onPullFinish();
            }
        });
        setBackgroundColor(Color.WHITE);
    }

    // 自定义回复动画
    public void startBackAnimation() {
        refreshLayout.setMoveWithFooter(false);
        refreshLayout.moveChildren(0);
        refreshLayout.setDispatchPullTouchAble(false);
        objectAnimator.setFloatValues(getY(), getY() + getMeasuredHeight());
        objectAnimator.start();
    }

    @Override
    public void onPullChange(float percent) {
        onPullHolding();

       // 判断是否处在 拖拽的状态
        if (refreshLayout.isDragDown() || refreshLayout.isDragUp()) {
            return;
        }
        if (!refreshLayout.isTargetAbleScrollDown()) {
            refreshLayout.autoLoading();
        }
    }

    @Override
    public void onPullHoldTrigger() {
    }

    @Override
    public void onPullHoldUnTrigger() {
    }

    @Override
    public void onPullHolding() {
        if (loadingView.getVisibility() != VISIBLE) {
            loadingView.smoothToShow();
            tv.setText("loading...");
        }
    }

    @Override
    public void onPullFinish() {
        tv.setText("loading finish");
        loadingView.smoothToHide();
    }

    @Override
    public void onPullReset() {
        tv.setText("drag");
    }
}
