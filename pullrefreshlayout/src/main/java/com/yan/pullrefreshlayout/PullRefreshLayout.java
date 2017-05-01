package com.yan.pullrefreshlayout;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;

/**
 * Created by yan on 2017/4/11.
 */
public class PullRefreshLayout extends FrameLayout implements NestedScrollingParent {
    private NestedScrollingParentHelper parentHelper;

    /**
     * refresh header
     */
    private PullRefreshView headerView;
    /**
     * refresh footer
     */
    private PullRefreshView footerView;

    /**
     * current refreshing state 1:refresh 2:loadMore
     */
    private int refreshState = 0;

    /**
     * drag move distance
     */
    private volatile int moveDistance = 0;

    /**
     * refresh target view
     */
    private View targetView;

    /**
     * header or footer height
     */
    private float pullViewHeight = 60;

    /**
     * max height drag
     */
    private float pullFlowHeight = 0;

    /**
     * the ratio for final distance for drag
     */
    private float dragDampingRatio = 0.6f;

    /**
     * animation during adjust value
     */
    private float duringAdjustValue = 0.4f;

    /**
     * switch refresh enable
     */
    private boolean pullRefreshEnable = true;

    /**
     * switch loadMore enable
     */
    private boolean pullLoadMoreEnable = true;

    /**
     * refreshState is isRefreshing
     */
    private boolean isRefreshing = false;

    /**
     * make sure header or footer hold trigger one time
     */
    private boolean pullStateControl = true;

    /**
     * is just use for twinkLayout
     */
    private boolean isUseAsTwinkLayout = false;

    /**
     * has called the method refreshComplete or loadMoreComplete
     */
    private boolean isResetTrigger = false;

    /**
     * refresh back time
     * if the value equals 0, the field duringAdjustValue will be work
     */
    private long refreshBackTime = 350;

    private OnRefreshListener onRefreshListener;

    private ValueAnimator currentAnimation;

    public PullRefreshLayout(Context context) {
        super(context);
        init(context);
    }

    public PullRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PullRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        parentHelper = new NestedScrollingParentHelper(this);
        pullViewHeight = dipToPx(context, pullViewHeight);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getChildCount() > 1) {
            throw new RuntimeException("PullRefreshLayout should not have more than one child");
        } else if (getChildCount() == 0) {
            throw new RuntimeException("PullRefreshLayout should have one child");
        }
        targetView = getChildAt(0);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup
                .LayoutParams.MATCH_PARENT, (int) pullViewHeight);
        if (!isUseAsTwinkLayout && headerView != null) {
            addView(headerView, layoutParams);
        }
        if (!isUseAsTwinkLayout && footerView != null) {
            addView(footerView, layoutParams);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!isUseAsTwinkLayout && headerView != null) {
            headerView.layout(left, (int) (-pullViewHeight), right, 0);
        }
        if (!isUseAsTwinkLayout && footerView != null) {
            footerView.layout(left, bottom - top, right, (int) (bottom - top + pullViewHeight));
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if ((!pullRefreshEnable && !pullLoadMoreEnable)) {
            return false;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        if (currentAnimation != null) {
            currentAnimation.cancel();
        }
        return true;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        parentHelper.onNestedScrollAccepted(child, target, axes);
    }

    /**
     * handler : refresh or loading
     *
     * @param child : child view of PullRefreshLayout,RecyclerView or Scroller
     */
    @Override
    public void onStopNestedScroll(View child) {
        parentHelper.onStopNestedScroll(child);
        handleState();
    }

    /**
     * with child view to processing move events
     *
     * @param target   the child view
     * @param dx       move x
     * @param dy       move y
     * @param consumed parent consumed move distance
     */
    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        if ((!pullRefreshEnable && !pullLoadMoreEnable)) {
            return;
        }
        if (Math.abs(dy) > 200) {
            return;
        }
        if (dy > 0 && moveDistance > 0) {
            if (moveDistance - dy < 0) {
                onScroll(-moveDistance);
            } else {
                onScroll(-dy);
            }
            consumed[1] += dy;
        }
        if (dy < 0 && moveDistance < 0) {
            if (moveDistance - dy > 0) {
                onScroll(-moveDistance);
            } else {
                onScroll(-dy);
            }
            consumed[1] += dy;
        }
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        if (!isRefreshing
                ||(dyUnconsumed < 0 && refreshState == 1)
                ||(dyUnconsumed > 0 && refreshState == 2)) {
            dyUnconsumed = (int) (dyUnconsumed * dragDampingRatio);
            onScroll(-dyUnconsumed);
        }
    }

    @Override
    public int getNestedScrollAxes() {
        return parentHelper.getNestedScrollAxes();
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        return false;
    }

    /**
     * dell the nestedScroll
     *
     * @param distanceY move distance of Y
     */
    private void onScroll(float distanceY) {
        if (pullFlowHeight != 0) {
            if (moveDistance + distanceY > pullFlowHeight) {
                moveDistance = (int) pullFlowHeight;
            } else if (moveDistance + distanceY < -pullFlowHeight) {
                moveDistance = -(int) pullFlowHeight;
            } else {
                moveDistance += distanceY;
            }
        } else {
            moveDistance += distanceY;
        }

        moveChildren(moveDistance);

        if (moveDistance >= 0) {
            if (!isUseAsTwinkLayout && headerView != null) {
                headerView.onPullChange(moveDistance / pullViewHeight);
            }
            if (moveDistance >= pullViewHeight) {
                if (pullStateControl) {
                    pullStateControl = false;
                    if (!isUseAsTwinkLayout && headerView != null && !isRefreshing) {
                        headerView.onPullHoldTrigger();
                    }
                }
            } else {
                if (!pullStateControl) {
                    pullStateControl = true;
                    if (!isUseAsTwinkLayout && headerView != null && !isRefreshing) {
                        headerView.onPullHoldUnTrigger();
                    }
                }
            }
        } else {
            if (!isUseAsTwinkLayout && headerView != null) {
                footerView.onPullChange(moveDistance / pullViewHeight);
            }
            if (moveDistance <= -pullViewHeight) {
                if (pullStateControl) {
                    pullStateControl = false;
                    if (!isUseAsTwinkLayout && footerView != null && !isRefreshing) {
                        footerView.onPullHoldTrigger();
                    }
                }
            } else {
                if (!pullStateControl) {
                    pullStateControl = true;
                    if (!isUseAsTwinkLayout && footerView != null && !isRefreshing) {
                        footerView.onPullHoldUnTrigger();
                    }
                }
            }
        }
    }

    /**
     * move children
     */
    private void moveChildren(float distance) {
        if (!isUseAsTwinkLayout && headerView != null) {
            headerView.setTranslationY(distance);
        }
        if (!isUseAsTwinkLayout && footerView != null) {
            footerView.setTranslationY(distance);
        }
        targetView.setTranslationY(distance);
    }

    /**
     * decide on the action refresh or loadMore
     */
    private void handleState() {
        if (pullRefreshEnable) {
            if (!isResetTrigger && !isUseAsTwinkLayout && moveDistance >= pullViewHeight) {
                startRefresh(moveDistance);
            } else if (!isRefreshing && moveDistance > 0) {
                resetHeaderView(moveDistance);
            } else if (!isRefreshing && moveDistance == 0) {
                resetRefreshState();
            }
        }

        if (pullLoadMoreEnable) {
            if (!isResetTrigger && !isUseAsTwinkLayout && moveDistance <= -pullViewHeight) {
                startLoadMore(moveDistance);
            } else if (!isRefreshing && moveDistance < 0) {
                resetFootView(moveDistance);
            } else if (!isRefreshing && moveDistance == 0) {
                resetLoadMoreState();
            }
        }
    }

    /**
     * start Refresh
     *
     * @param headerViewHeight
     */
    private void startRefresh(int headerViewHeight) {
        if (headerView != null) {
            headerView.onPullHolding();
        }
        ValueAnimator animator = ValueAnimator.ofInt(headerViewHeight, (int) pullViewHeight);
        currentAnimation = animator;
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                moveDistance = (Integer) animation.getAnimatedValue();
                if (!isUseAsTwinkLayout && headerView != null) {
                    headerView.onPullChange(moveDistance / pullViewHeight);
                }
                moveChildren(moveDistance);
            }
        });
        animator.addListener(new RefreshAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (onRefreshListener != null && !isRefreshing) {
                    onRefreshListener.onRefresh();
                    isRefreshing = true;
                    refreshState = 1;
                }
            }
        });
        if (headerViewHeight == 0) {
            animator.setDuration(refreshBackTime);
        } else {
            animator.setDuration((long) (Math.pow(moveDistance * 4, 0.6) / duringAdjustValue));
        }
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }

    /**
     * reset refresh refreshState
     *
     * @param headerViewHeight
     */
    private void resetHeaderView(int headerViewHeight) {
        if (headerViewHeight == 0) {
            resetRefreshState();
            return;
        }
        ValueAnimator animator = ValueAnimator.ofInt(headerViewHeight, 0);
        currentAnimation = animator;
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                moveDistance = (Integer) animation.getAnimatedValue();
                if (!isUseAsTwinkLayout && headerView != null) {
                    headerView.onPullChange(moveDistance / pullViewHeight);
                }
                moveChildren(moveDistance);
            }
        });
        animator.addListener(new RefreshAnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (!isUseAsTwinkLayout && headerView != null && isRefreshing) {
                    headerView.onPullFinish();
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                resetRefreshState();
            }
        });
        if (refreshBackTime != 0 && !isUseAsTwinkLayout) {
            animator.setDuration(refreshBackTime);
        } else {
            animator.setDuration((long) (Math.pow(moveDistance * 4, 0.6) / duringAdjustValue));
        }
        animator.start();
    }

    private void resetRefreshState() {
        if (!isUseAsTwinkLayout && headerView != null) {
            headerView.onPullReset();
        }
        isRefreshing = false;
        moveDistance = 0;
        refreshState = 0;
        isResetTrigger = false;
        pullStateControl = true;
    }

    /**
     * start loadMore
     *
     * @param loadMoreViewHeight
     */
    private void startLoadMore(int loadMoreViewHeight) {
        if (footerView != null) {
            footerView.onPullHolding();
        }
        ValueAnimator animator = ValueAnimator.ofInt(loadMoreViewHeight, -(int) pullViewHeight);
        currentAnimation = animator;
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                moveDistance = (Integer) animation.getAnimatedValue();
                if (!isUseAsTwinkLayout && headerView != null) {
                    footerView.onPullChange(moveDistance / pullViewHeight);
                }
                moveChildren(moveDistance);
            }
        });
        animator.addListener(new RefreshAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (onRefreshListener != null && !isRefreshing) {
                    onRefreshListener.onLoading();
                    isRefreshing = true;
                    refreshState = 2;
                }
            }
        });
        animator.setDuration((long) (Math.pow(Math.abs(moveDistance) * 4, 0.6) / duringAdjustValue));
        animator.start();
    }

    /**
     * reset loadMore refreshState
     *
     * @param loadMoreViewHeight
     */
    private void resetFootView(int loadMoreViewHeight) {
        if (loadMoreViewHeight == 0) {
            resetLoadMoreState();
            return;
        }
        ValueAnimator animator = ValueAnimator.ofInt(loadMoreViewHeight, 0);
        currentAnimation = animator;
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                moveDistance = (Integer) animation.getAnimatedValue();
                if (!isUseAsTwinkLayout && headerView != null) {
                    footerView.onPullChange(moveDistance / pullViewHeight);
                }
                moveChildren(moveDistance);
            }
        });
        animator.addListener(new RefreshAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                resetLoadMoreState();
            }

            @Override
            public void onAnimationStart(Animator animation) {
                if (!isUseAsTwinkLayout && footerView != null && isRefreshing) {
                    footerView.onPullFinish();
                }
            }
        });
        if (refreshBackTime != 0 && !isUseAsTwinkLayout) {
            animator.setDuration(refreshBackTime);
        } else {
            animator.setDuration((long) (Math.pow(Math.abs(moveDistance) * 4, 0.6) / duringAdjustValue));
        }
        animator.start();
    }

    private void resetLoadMoreState() {
        if (!isUseAsTwinkLayout && footerView != null) {
            footerView.onPullReset();
        }
        isRefreshing = false;
        moveDistance = 0;
        refreshState = 0;
        isResetTrigger = false;
        pullStateControl = true;
    }

    public void autoRefresh() {
        if (targetView == null || isUseAsTwinkLayout) {
            return;
        }
        startRefresh(0);
    }

    public float dipToPx(Context context, float value) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, metrics);
    }

    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        this.onRefreshListener = onRefreshListener;
    }

    /**
     * callback on refresh finish
     */
    public void refreshComplete() {
        if (moveDistance >= 0 && refreshState == 1) {
            isResetTrigger = true;
            resetHeaderView(moveDistance);
        }
    }

    /**
     * Callback on loadMore finish
     */
    public void loadMoreComplete() {
        if (moveDistance <= 0 && refreshState == 2) {
            isResetTrigger = true;
            resetFootView(moveDistance);
        }
    }

    public void setUseAsTwinkLayout(boolean useAsTwinkLayout) {
        isUseAsTwinkLayout = useAsTwinkLayout;
    }

    public boolean isLoadMoreEnable() {
        return pullLoadMoreEnable;
    }

    public void setLoadMoreEnable(boolean mPullLoadEnable) {
        this.pullLoadMoreEnable = mPullLoadEnable;
    }

    public boolean isRefreshEnable() {
        return pullRefreshEnable;
    }

    public void setRefreshEnable(boolean mPullRefreshEnable) {
        this.pullRefreshEnable = mPullRefreshEnable;
    }

    public boolean isRefreshing() {
        return isRefreshing;
    }

    public void setHeaderView(PullRefreshView header) {
        headerView = header;
    }

    public void setFooterView(PullRefreshView footer) {
        footerView = footer;
    }

    public void setPullViewHeight(float pullViewHeight) {
        this.pullViewHeight = pullViewHeight;
    }

    public void setPullFlowHeight(float pullFlowHeight) {
        this.pullFlowHeight = pullFlowHeight;
    }

    public void setDragDampingRatio(float dragDampingRatio) {
        this.dragDampingRatio = dragDampingRatio;
    }

    public void setDuringAdjustValue(float duringAdjustValue) {
        this.duringAdjustValue = duringAdjustValue;
    }

    public void setRefreshBackTime(long refreshBackTime) {
        this.refreshBackTime = refreshBackTime;
    }

    public static interface OnPullListener {
        void onPullChange(float percent);

        void onPullReset();

        void onPullHoldTrigger();

        void onPullHoldUnTrigger();

        void onPullHolding();

        void onPullFinish();
    }

    public static abstract class OnRefreshListener {
        public void onRefresh() {
        }

        public void onLoading() {
        }
    }

    private static class RefreshAnimatorListener implements Animator.AnimatorListener {

        @Override
        public void onAnimationStart(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
        }

        @Override
        public void onAnimationCancel(Animator animation) {
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }
    }
}
