package com.yan.pullrefreshlayout;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AbsListView;
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
     * drag move distance
     */
    private float moveDistance = 0;

    /**
     * refresh target view
     */
    private View targetView;

    /**
     * drag refresh
     */
    private static final int STATE_PULL_REFRESH = 0;
    /**
     * drag loadMore
     */
    private static final int STATE_LOAD_MORE = 1;

    /**
     * switch refresh enable
     */
    private boolean pullRefreshEnable = true;

    /**
     * switch loadMore enable
     */
    private boolean pullLoadMoreEnable = true;

    /**
     * state is isRefreshing
     */
    volatile private boolean isRefreshing = false;

    /**
     * make sure header or footer hold trigger one time
     */
    private boolean pullStateControl = true;

    /**
     * header or footer height
     */
    private float pullViewHeight = 60;

    /**
     * max height drag
     */
    private float pullFlowHeight = 0;

    /**
     * drag current action
     */
    private int currentState = -1;

    /**
     * make sure the current state
     */
    private boolean isGettingState = false;

    /**
     * the ratio for final distance for drag
     */
    private float dragDampingRatio = 0.6f;

    /**
     * is just use for twinkLayout
     */
    private boolean isUseAsTwinkLayout = false;

    /**
     * animation during adjust value
     */
    private float duringAdjustValue = 0.4f;

    /**
     * refresh back time
     * if the value equals 0, the field duringAdjustValue will be work
     */
    private long refreshBackTime = 350;

    private OnRefreshListener onRefreshListener;

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
        if (isRefreshing) {
            return false;
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

        if (!isGettingState) {
            if (dy < 0 && !canChildScrollUp()) {
                currentState = STATE_PULL_REFRESH;
                isGettingState = true;
            } else if (dy > 0 && !canChildScrollDown()) {
                currentState = STATE_LOAD_MORE;
                isGettingState = true;
            }
        }

        if (currentState == STATE_PULL_REFRESH) {
            if (dy > 0) {
                onScroll(-dy);
                consumed[1] += dy;
            }
        } else if (currentState == STATE_LOAD_MORE) {
            if (dy < 0) {
                onScroll(-dy);
                consumed[1] += dy;
            }
        }
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        if (currentState == STATE_PULL_REFRESH
                || currentState == STATE_LOAD_MORE) {
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
        if (!canChildScrollUp() && pullRefreshEnable && currentState == STATE_PULL_REFRESH) {
            // Pull Refresh
            moveDistance += distanceY;

            if (moveDistance < 0) {
                moveDistance = 0;
            }
            if (pullFlowHeight != 0 && moveDistance > pullFlowHeight) {
                moveDistance = pullFlowHeight;
            }
            if (moveDistance == 0) {
                isGettingState = false;
                currentState = -1;
            }
            if (moveDistance >= pullViewHeight) {
                if (pullStateControl) {
                    pullStateControl = false;
                    if (!isUseAsTwinkLayout && headerView != null) {
                        headerView.onPullHoldTrigger();
                    }
                }
            } else {
                if (!pullStateControl) {
                    pullStateControl = true;
                    if (!isUseAsTwinkLayout && headerView != null) {
                        headerView.onPullHoldUnTrigger();
                    }
                }
            }
            if (!isUseAsTwinkLayout && headerView != null) {
                headerView.onPullChange(moveDistance / pullViewHeight);
            }
            moveChildren(moveDistance);
        } else if (!canChildScrollDown() && pullLoadMoreEnable && currentState == STATE_LOAD_MORE) {
            // Load more
            moveDistance -= distanceY;
            if (moveDistance < 0) {
                moveDistance = 0;
            }
            if (pullFlowHeight != 0 && moveDistance > pullFlowHeight) {
                moveDistance = pullFlowHeight;
            }

            if (moveDistance == 0) {
                isGettingState = false;
                currentState = -1;
            }
            if (moveDistance >= pullViewHeight) {
                if (pullStateControl) {
                    pullStateControl = false;
                    if (!isUseAsTwinkLayout && footerView != null) {
                        footerView.onPullHoldTrigger();
                    }
                }
            } else {
                if (!pullStateControl) {
                    pullStateControl = true;
                    if (!isUseAsTwinkLayout && footerView != null) {
                        footerView.onPullHoldUnTrigger();
                    }
                }
            }
            if (!isUseAsTwinkLayout && footerView != null) {
                footerView.onPullChange(moveDistance / pullViewHeight);
            }
            moveChildren(-moveDistance);
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
        if (isRefreshing) {
            return;
        }
        isGettingState = false;

        if (pullRefreshEnable && currentState == STATE_PULL_REFRESH) {
            if (!isUseAsTwinkLayout && moveDistance >= pullViewHeight) {
                startRefresh((int) moveDistance);
            } else if (moveDistance > 0) {
                resetHeaderView((int) moveDistance);
            } else {
                resetRefreshState();
            }
        }

        if (pullLoadMoreEnable && currentState == STATE_LOAD_MORE) {
            if (!isUseAsTwinkLayout && moveDistance >= pullViewHeight) {
                startLoadMore((int) moveDistance);
            } else if (moveDistance > 0) {
                resetFootView((int) moveDistance);
            } else {
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
        isRefreshing = true;
        if (headerView != null) {
            headerView.onPullHolding();
        }
        ValueAnimator animator = ValueAnimator.ofFloat(headerViewHeight, pullViewHeight);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                moveDistance = (int) ((Float) animation.getAnimatedValue()).floatValue();
                moveChildren(moveDistance);
            }
        });
        animator.addListener(new RefreshAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (onRefreshListener != null) {
                    onRefreshListener.onRefresh();
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
     * reset refresh state
     *
     * @param headerViewHeight
     */
    private void resetHeaderView(int headerViewHeight) {
        ValueAnimator animator = ValueAnimator.ofFloat(headerViewHeight, 0);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                moveDistance = (int) ((Float) animation.getAnimatedValue()).floatValue();
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
        isGettingState = false;
        pullStateControl = true;
        currentState = -1;
    }

    /**
     * start loadMore
     *
     * @param loadMoreViewHeight
     */
    private void startLoadMore(int loadMoreViewHeight) {
        isRefreshing = true;
        if (footerView != null) {
            footerView.onPullHolding();
        }
        ValueAnimator animator = ValueAnimator.ofFloat(loadMoreViewHeight, pullViewHeight);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                moveDistance = (int) ((Float) animation.getAnimatedValue()).floatValue();
                moveChildren(-moveDistance);
            }
        });
        animator.addListener(new RefreshAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (onRefreshListener != null) {
                    onRefreshListener.onLoading();
                }
            }
        });
        animator.setDuration((long) (Math.pow(moveDistance * 4, 0.6) / duringAdjustValue));
        animator.start();
    }

    /**
     * reset loadMore state
     *
     * @param loadMoreViewHeight
     */
    private void resetFootView(int loadMoreViewHeight) {
        ValueAnimator animator = ValueAnimator.ofFloat(loadMoreViewHeight, 0);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                moveDistance = (int) ((Float) animation.getAnimatedValue()).floatValue();
                moveChildren(-moveDistance);
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
            animator.setDuration((long) (Math.pow(moveDistance * 4, 0.6) / duringAdjustValue));
        }
        animator.start();
    }

    private void resetLoadMoreState() {
        if (!isUseAsTwinkLayout && footerView != null) {
            footerView.onPullReset();
        }
        isRefreshing = false;
        moveDistance = 0;
        isGettingState = false;
        pullStateControl = true;
        currentState = -1;
    }

    public void autoRefresh() {
        if (targetView == null) return;
        currentState = STATE_PULL_REFRESH;
        isGettingState = true;
        startRefresh(0);
    }

    /**
     * whether child view can scroll up
     *
     * @return
     */
    public boolean canChildScrollUp() {
        if (targetView == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT < 14) {
            if (targetView instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) targetView;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(targetView, -1) || targetView.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(targetView, -1);
        }
    }

    /**
     * whether child view can scroll down
     *
     * @return
     */
    public boolean canChildScrollDown() {
        if (targetView == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT < 14) {
            if (targetView instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) targetView;
                if (absListView.getChildCount() > 0) {
                    int lastChildBottom = absListView.getChildAt(absListView.getChildCount() - 1)
                            .getBottom();
                    return absListView.getLastVisiblePosition() == absListView.getAdapter().getCount() - 1
                            && lastChildBottom <= absListView.getMeasuredHeight();
                } else {
                    return false;
                }

            } else {
                return ViewCompat.canScrollVertically(targetView, 1) || targetView.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(targetView, 1);
        }
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
        if (currentState == STATE_PULL_REFRESH) {
            resetHeaderView((int) moveDistance);
        }
    }

    /**
     * Callback on loadMore finish
     */
    public void loadMoreComplete() {
        if (currentState == STATE_LOAD_MORE) {
            resetFootView((int) moveDistance);
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
