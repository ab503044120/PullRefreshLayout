package com.yan.refreshloadlayouttest.widget.fungame;

import android.content.Context;
import android.widget.FrameLayout;

import com.yan.pullrefreshlayout.PullRefreshLayout;
import com.yan.pullrefreshlayout.ShowGravity;


/**
 * 游戏 header
 * Created by SCWANG on 2017/6/17.
 */

public class FunGameBase extends FrameLayout implements PullRefreshLayout.OnPullListener {

    //<editor-fold desc="Field">
    protected int mHeaderHeight;
    protected int mScreenHeightPixels;
    protected boolean mManualOperation;
    protected PullRefreshLayout refreshLayout;
    //</editor-fold>

    //<editor-fold desc="View">
    public FunGameBase(Context context, PullRefreshLayout refreshLayout) {
        super(context);
        this.refreshLayout = refreshLayout;
        initView(context);
    }

    private void initView(Context context) {
        mScreenHeightPixels = context.getResources().getDisplayMetrics().heightPixels;
        mHeaderHeight = (int) (mScreenHeightPixels * 0.2f);
        refreshLayout.setRefreshTriggerDistance(mHeaderHeight);
        refreshLayout.setHeaderShowGravity(ShowGravity.FOLLOW);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    protected void onManualOperationMove(float percent) {

    }

    @Override
    public void onPullChange(float percent) {
        if (mManualOperation && refreshLayout.isHoldingFinishTrigger() && !refreshLayout.isDragUp() && !refreshLayout.isDragDown()) {
            onPullFinish();
        }
        if (mManualOperation) {
            if (percent == 1) {
                refreshLayout.setDispatchPullTouchAble(true);
                refreshLayout.setMoveWithHeader(false);
            }
            if (percent < 1) {
                refreshLayout.moveChildren(refreshLayout.getRefreshTriggerDistance());
            }

            onManualOperationMove(1 + (percent - 1) * 0.6F);
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
        mManualOperation = true;
        refreshLayout.setDispatchPullTouchAble(false);
        refreshLayout.setDragDampingRatio(1);
    }

    @Override
    public void onPullFinish() {
        if (refreshLayout.isDragDown() || refreshLayout.isDragUp()) {
            refreshLayout.cancelAllAnimation();
        } else {
            mManualOperation = false;
            refreshLayout.setDragDampingRatio(0.6F);
            refreshLayout.setMoveWithHeader(true);
        }
    }

    @Override
    public void onPullReset() {
    }
}
