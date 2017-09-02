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
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wang.avi.AVLoadingIndicatorView;
import com.yan.pullrefreshlayout.PullRefreshLayout;
import com.yan.pullrefreshlayout.ShowGravity;
import com.yan.refreshloadlayouttest.R;

/**
 * Created by yan on 2017/8/14.
 * <p>
 * 使用这个footer却确认调用以下两个方法
 * refreshLayout.setLoadTriggerDistance(120); 主动设置加载更多的触发距离
 * 设置 footerShowState（默认 为STATE_FOLLOW） 为 STATE_FOLLOW
 */

public class ClassicLoadView extends FrameLayout implements PullRefreshLayout.OnPullListener {
    private TextView tv;
    private AVLoadingIndicatorView loadingView;
    private PullRefreshLayout refreshLayout;
    private ObjectAnimator objectAnimator;

    public ClassicLoadView(@NonNull Context context, final PullRefreshLayout refreshLayout) {
        super(context);
        this.refreshLayout = refreshLayout;
        this.refreshLayout.setFooterFront(true);
        this.refreshLayout.setFooterShowGravity(ShowGravity.FOLLOW);
        // 设置 布局 为 match_parent
        setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        setBackgroundColor(Color.WHITE);
        initView();
        animationInit();
    }

    // 动画初始化
    private void animationInit() {
        objectAnimator = ObjectAnimator.ofFloat(this, "y", 0, 0);
        objectAnimator.setDuration(300);

        objectAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                refreshLayout.loadMoreComplete();
                refreshLayout.setMoveWithFooter(true);
                refreshLayout.cancelTouchEvent();
                refreshLayout.setDispatchPullTouchAble(true);
            }
        });
    }

    // 自定义回复动画
    public void startBackAnimation() {
        // 记录refreshLayout移动距离
        int moveDistance = refreshLayout.getMoveDistance();
        if (moveDistance >= 0) {// moveDistance大于等于0时不主动处理
            refreshLayout.loadMoreComplete();
            return;
        }
        // 阻止refreshLayout的默认事件分发
        refreshLayout.setDispatchPullTouchAble(false);
        // 先设置footer不跟随移动
        refreshLayout.setMoveWithFooter(false);
        // 再设置内容移动到0的位置
        refreshLayout.moveChildren(0);
        // 设置事件为ACTION_CANCEL
        refreshLayout.cancelTouchEvent();
        // 调用自定义footer动画
        objectAnimator.setFloatValues(getY(), getY() - moveDistance);
        objectAnimator.start();

    }

    public void loadFinish() {
        if (refreshLayout.isLoadMoreEnable()) {
            refreshLayout.setLoadMoreEnable(false);
            refreshLayout.setAutoLoadingEnable(false);
            refreshLayout.loadMoreComplete();
            tv.setText("no more data");
            loadingView.smoothToHide();
        }
    }

    private void initView() {
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
        loadingView.setIndicatorColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
    }

    @Override
    public void onPullChange(float percent) {
        onPullHolding();
        // 判断是否处在 拖拽的状态
        if (refreshLayout.isDragDown() || refreshLayout.isDragUp() || !refreshLayout.isLoadMoreEnable()) {
            return;
        }
        if (!refreshLayout.isTargetAbleScrollDown() && !refreshLayout.isLoading()) {
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
        if (loadingView.getVisibility() != VISIBLE && refreshLayout.isLoadMoreEnable()) {
            loadingView.smoothToShow();
            tv.setText("loading...");
        }
    }

    @Override
    public void onPullFinish() {
        Log.e("onPullFinish", "onPullFinish: ");
        if (refreshLayout.isLoadMoreEnable()) {
            tv.setText("loading finish");
            loadingView.smoothToHide();
        }
    }

    @Override
    public void onPullReset() {
        Log.e("onPullReset", "onPullReset: ");
    }
}
