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
    private OnRefreshListener onRefreshListener;

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
     * drag action refresh
     */
    private static final int ACTION_PULL_REFRESH = 0;
    /**
     * drag action loadMore
     */
    private static final int ACTION_LOAD_MORE = 1;

    /**
     * switch refresh enable
     */
    private boolean pullRefreshEnable = true;

    /**
     * switch loadMore enable
     */
    private boolean pullLoadEnable = true;

    /**
     * state is refreshing
     */
    volatile private boolean refreshing = false;

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
    private int currentAction = -1;

    /**
     * make sure the current action
     */
    private boolean isConfirm = false;

    /**
     * the ratio for final distance for drag
     */
    private float dragDampingRatio = 0.6f;

    /**
     * is just use for twinkLayout
     */
    private boolean isUseForTwinkLayout = false;

    /**
     * victory pixel peer millisecond
     */
    private float victoryPixelPeerMillisecond = 2;

    /**
     * refresh back time
     * if the value equals 0, the field victoryPixelPeerMillisecond will be work
     */
    private long refreshBackTime = 350;

    public PullRefreshLayout(Context context) {
        super(context);
        initAttrs(context);
    }

    public PullRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(context);
    }

    public PullRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context);
    }

    private void initAttrs(Context context) {
        if (getChildCount() > 1) {
            throw new RuntimeException("PullRefreshLayout should not have more than one child");
        }

        parentHelper = new NestedScrollingParentHelper(this);
        pullViewHeight = dipToPx(context, pullViewHeight);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        targetView = getChildAt(0);
        if (!isUseForTwinkLayout && headerView != null) {
            addView(headerView, new FrameLayout.LayoutParams(ViewGroup
                    .LayoutParams.MATCH_PARENT, (int) pullViewHeight));
        }
        if (!isUseForTwinkLayout && footerView != null) {
            addView(footerView, new FrameLayout.LayoutParams(ViewGroup
                    .LayoutParams.MATCH_PARENT, (int) pullViewHeight));
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!isUseForTwinkLayout && headerView != null) {
            headerView.layout(left, (int) (-pullViewHeight), right, 0);
        }
        if (!isUseForTwinkLayout && footerView != null) {
            footerView.layout(left, bottom - top, right, (int) (bottom - top + pullViewHeight));
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if ((!pullRefreshEnable && !pullLoadEnable)) {
            return false;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        if (refreshing) {
            return false;
        }
        return true;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        parentHelper.onNestedScrollAccepted(child, target, axes);
    }

    /**
     * callback on TouchEvent.ACTION_CANCEL or TouchEvent.ACTION_UP
     * handler : refresh or loading
     *
     * @param child : child view of PullRefreshLayout,RecyclerView or Scroller
     */
    @Override
    public void onStopNestedScroll(View child) {
        parentHelper.onStopNestedScroll(child);
        handlerAction();
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
        if ((!pullRefreshEnable && !pullLoadEnable)) {
            return;
        }
        // Prevent Layout shake
        if (Math.abs(dy) > 200) {
            return;
        }
        Log.e("onNestedPreScroll: dy:", dy + "");
        if (!isConfirm) {
            if (dy < 0 && !canChildScrollUp()) {
                currentAction = ACTION_PULL_REFRESH;
                isConfirm = true;
            } else if (dy > 0 && !canChildScrollDown()) {
                currentAction = ACTION_LOAD_MORE;
                isConfirm = true;
            }
        }

        if (currentAction == ACTION_PULL_REFRESH) {
            if (dy > 0) {
                moveContainer(-dy);
                consumed[1] += dy;
            }
        } else if (currentAction == ACTION_LOAD_MORE) {
            if (dy < 0) {
                moveContainer(-dy);
                consumed[1] += dy;
            }
        }
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        if (currentAction == ACTION_PULL_REFRESH
                || currentAction == ACTION_LOAD_MORE) {
            dyUnconsumed = (int) (dyUnconsumed * dragDampingRatio);
            moveContainer(-dyUnconsumed);
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
     * move container
     *
     * @param distanceY move distance of Y
     */
    private boolean moveContainer(float distanceY) {
        if (refreshing) {
            return false;
        }

        if (!canChildScrollUp() && pullRefreshEnable && currentAction == ACTION_PULL_REFRESH) {
            // Pull Refresh
            moveDistance += distanceY;

            if (moveDistance < 0) {
                moveDistance = 0;
            }
            if (pullFlowHeight != 0 && moveDistance > pullFlowHeight) {
                moveDistance = pullFlowHeight;
            }
            if (moveDistance == 0) {
                isConfirm = false;
                currentAction = -1;
            }
            if (moveDistance >= pullViewHeight) {
                if (pullStateControl) {
                    pullStateControl = false;
                    if (!isUseForTwinkLayout && headerView != null) {
                        headerView.onPullHoldTrigger();
                    }
                }
            } else {
                if (!pullStateControl) {
                    pullStateControl = true;
                    if (!isUseForTwinkLayout && headerView != null) {
                        headerView.onPullHoldUnTrigger();
                    }
                }
            }
            if (!isUseForTwinkLayout && headerView != null) {
                headerView.onPullChange(moveDistance / pullViewHeight);
            }
            moveView(moveDistance);
            return true;
        } else if (!canChildScrollDown() && pullLoadEnable && currentAction == ACTION_LOAD_MORE) {
            // Load more
            moveDistance -= distanceY;
            if (moveDistance < 0) {
                moveDistance = 0;
            }
            if (pullFlowHeight != 0 && moveDistance > pullFlowHeight) {
                moveDistance = pullFlowHeight;
            }

            if (moveDistance == 0) {
                isConfirm = false;
                currentAction = -1;
            }
            if (moveDistance >= pullViewHeight) {
                if (pullStateControl) {
                    pullStateControl = false;
                    if (!isUseForTwinkLayout && footerView != null) {
                        footerView.onPullHoldTrigger();
                    }
                }
            } else {
                if (!pullStateControl) {
                    pullStateControl = true;
                    if (!isUseForTwinkLayout && footerView != null) {
                        footerView.onPullHoldUnTrigger();
                    }
                }
            }
            if (!isUseForTwinkLayout && footerView != null) {
                footerView.onPullChange(moveDistance / pullViewHeight);
            }
            moveView(-moveDistance);
            return true;
        }
        return false;
    }

    /**
     * move children
     */
    private void moveView(float distance) {
        if (!isUseForTwinkLayout && headerView != null) {
            headerView.setTranslationY(distance);
        }
        if (!isUseForTwinkLayout && footerView != null) {
            footerView.setTranslationY(distance);
        }
        targetView.setTranslationY(distance);
    }

    /**
     * decide on the action refresh or loadMore
     */
    private void handlerAction() {

        if (refreshing) {
            return;
        }
        isConfirm = false;

        if (pullRefreshEnable && currentAction == ACTION_PULL_REFRESH) {
            if (!isUseForTwinkLayout && moveDistance >= pullViewHeight) {
                startRefresh((int) moveDistance);
            } else if (moveDistance > 0) {
                resetHeaderView((int) moveDistance);
            } else {
                resetRefreshState();
            }
        }

        if (pullLoadEnable && currentAction == ACTION_LOAD_MORE) {
            if (!isUseForTwinkLayout && moveDistance >= pullViewHeight) {
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
        refreshing = true;
        if (headerView != null) {
            headerView.onPullHolding();
        }
        ValueAnimator animator = ValueAnimator.ofFloat(headerViewHeight, pullViewHeight);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                moveDistance = (int) ((Float) animation.getAnimatedValue()).floatValue();
                moveView(moveDistance);
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
            animator.setDuration((long) (moveDistance / victoryPixelPeerMillisecond));
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
                moveView(moveDistance);
            }
        });
        animator.addListener(new RefreshAnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (!isUseForTwinkLayout && headerView != null && refreshing) {
                    headerView.onPullFinish();
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                resetRefreshState();
            }
        });
        if (refreshBackTime != 0 && !isUseForTwinkLayout) {
            animator.setDuration(refreshBackTime);
        } else {
            animator.setDuration((long) (moveDistance / victoryPixelPeerMillisecond));
        }
        animator.start();
    }

    private void resetRefreshState() {
        if (!isUseForTwinkLayout && headerView != null) {
            headerView.onPullReset();
        }
        refreshing = false;
        moveDistance = 0;
        isConfirm = false;
        pullStateControl = true;
        currentAction = -1;
    }

    /**
     * start loadMore
     *
     * @param loadMoreViewHeight
     */
    private void startLoadMore(int loadMoreViewHeight) {
        refreshing = true;
        if (footerView != null) {
            footerView.onPullHolding();
        }
        ValueAnimator animator = ValueAnimator.ofFloat(loadMoreViewHeight, pullViewHeight);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                moveDistance = (int) ((Float) animation.getAnimatedValue()).floatValue();
                moveView(-moveDistance);
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
        animator.setDuration((long) (moveDistance / victoryPixelPeerMillisecond));
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
                moveView(-moveDistance);
            }
        });
        animator.addListener(new RefreshAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                resetLoadMoreState();
            }

            @Override
            public void onAnimationStart(Animator animation) {
                if (!isUseForTwinkLayout && footerView != null && refreshing) {
                    footerView.onPullFinish();
                }
            }
        });
        if (refreshBackTime != 0 && !isUseForTwinkLayout) {
            animator.setDuration(refreshBackTime);
        } else {
            animator.setDuration((long) (moveDistance / victoryPixelPeerMillisecond));
        }
        animator.start();
    }

    private void resetLoadMoreState() {
        if (!isUseForTwinkLayout && footerView != null) {
            footerView.onPullReset();
        }
        refreshing = false;
        moveDistance = 0;
        isConfirm = false;
        pullStateControl = true;
        currentAction = -1;
    }

    public void autoRefresh() {
        if (targetView == null) return;
        currentAction = ACTION_PULL_REFRESH;
        isConfirm = true;
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
        if (currentAction == ACTION_PULL_REFRESH) {
            resetHeaderView((int) moveDistance);
        }
    }

    /**
     * Callback on loadMore finish
     */
    public void loadMoreComplete() {
        if (currentAction == ACTION_LOAD_MORE) {
            resetFootView((int) moveDistance);
        }
    }

    public void setUseForTwinkLayout(boolean useForTwinkLayout) {
        isUseForTwinkLayout = useForTwinkLayout;
    }

    public boolean isLoadMoreEnable() {
        return pullLoadEnable;
    }

    public void setLoadMoreEnable(boolean mPullLoadEnable) {
        this.pullLoadEnable = mPullLoadEnable;
    }

    public boolean isRefreshEnable() {
        return pullRefreshEnable;
    }

    public void setRefreshEnable(boolean mPullRefreshEnable) {
        this.pullRefreshEnable = mPullRefreshEnable;
    }

    public boolean isRefreshing() {
        return refreshing;
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

    public void setVictoryPixelPeerMillisecond(float victoryPixelPeerMillisecond) {
        this.victoryPixelPeerMillisecond = victoryPixelPeerMillisecond;
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
