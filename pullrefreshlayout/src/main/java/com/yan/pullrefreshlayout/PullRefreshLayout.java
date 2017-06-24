package com.yan.pullrefreshlayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.ScrollerCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
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
    private View headerView;

    /**
     * refresh footer
     */
    private View footerView;

    /**
     * current refreshing state 1:refresh 2:loadMore
     */
    private int refreshState = 0;

    /**
     * last Scroll Y
     */
    private int lastScrollY = 0;

    /**
     * twink during adjust value
     */
    private int adjustTwinkDuring = 2;

    /**
     * over scroll state
     */
    private int overScrollState = 0;

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
    private float headerHeight = 60;

    /**
     * header or footer height
     */
    private float footerHeight = 60;

    /**
     * max height drag
     */
    private float pullFlowHeight = -1;

    /**
     * the ratio for final distance for drag
     */
    private float dragDampingRatio = 0.6f;

    /**
     * move distance ratio for over scroll
     */
    private float overScrollDampingRatio = 0.4f;

    /**
     * animation during adjust value
     */
    private float duringAdjustValue = 10f;

    /**
     * animation during adjust value
     */
    private float currentVelocityY = 0;

    /**
     * switch refresh enable
     */
    private boolean pullRefreshEnable = true;

    /**
     * is Twink enable
     */
    private boolean pullTwinkEnable = true;

    /**
     * switch loadMore enable
     */
    private boolean pullLoadMoreEnable = false;

    /**
     * refreshState is isRefreshing
     */
    private boolean isRefreshing = false;

    /**
     * make sure header or footer hold trigger one time
     */
    private boolean pullStateControl = true;

    /**
     * has called the method refreshComplete or loadMoreComplete
     */
    private boolean isResetTrigger = false;

    /**
     * is able auto load more
     */
    private boolean autoLoadingEnable = false;

    /**
     * is able auto load more
     */
    private boolean autoLoadTrigger = false;

    /**
     * is over scroll trigger
     */
    private boolean isOverScrollTrigger = false;

    /**
     * on attached to window
     */
    private boolean onAttachedToWindow = false;

    /**
     * refresh back time
     * if the value equals -1, the field duringAdjustValue will be work
     */
    private long refreshBackTime = 350;

    private OnRefreshListener onRefreshListener;

    private ValueAnimator currentAnimation;

    private ValueAnimator scrollAnimation;

    private ScrollerCompat scroller;

    public PullRefreshLayout(Context context) {
        super(context);
        pullInit(context);
    }

    public PullRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        pullInit(context);
    }

    public PullRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        pullInit(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() > 1) {
            throw new RuntimeException("PullRefreshLayout should not have more than one child");
        } else if (getChildCount() == 0) {
            throw new RuntimeException("PullRefreshLayout should have one child");
        }
        targetView = getChildAt(0);
        initScroller();

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (onAttachedToWindow) return;
        onAttachedToWindow = true;
        if (headerView != null) {
            addView(headerView, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        }
        if (footerView != null) {
            addView(footerView, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        }
    }

    private void pullInit(Context context) {
        parentHelper = new NestedScrollingParentHelper(this);
        headerHeight = dipToPx(context, headerHeight);
        footerHeight = dipToPx(context, footerHeight);
    }

    private void initScroller() {
        if (pullTwinkEnable && scroller == null) {
            if (targetView instanceof RecyclerView) {
                scroller = ScrollerCompat.create(getContext(), getRecyclerDefaultInterpolator());
            } else {
                scroller = ScrollerCompat.create(getContext());
            }
        }
    }

    private Interpolator getRecyclerDefaultInterpolator() {
        return new Interpolator() {
            @Override
            public float getInterpolation(float t) {
                t -= 1.0f;
                return t * t * t * t * t + 1.0f;
            }
        };
    }

    /**
     * onOverScrollUp
     */
    private void onOverScrollUp() {
        overScrollState = 1;
    }

    /**
     * onOverScrollDown
     */
    private void onOverScrollDown() {
        overScrollState = 2;
        if (autoLoadingEnable && !isRefreshing && onRefreshListener != null && !autoLoadTrigger) {
            autoLoadTrigger = true;
            onRefreshListener.onLoading();
        }
    }

    @Override
    public void computeScroll() {
        if (scroller != null && scroller.computeScrollOffset()) {
            if (!isOverScrollTrigger && !canChildScrollUp() && canChildScrollDown()) {
                isOverScrollTrigger = true;
                onOverScrollUp();
            } else if (!isOverScrollTrigger && !canChildScrollDown() && canChildScrollUp()) {
                isOverScrollTrigger = true;
                onOverScrollDown();
            }

            int currY = scroller.getCurrY();
            int tempDistance = currY - lastScrollY;
            if (currentVelocityY > 0 && moveDistance >= 0) {
                if (moveDistance - tempDistance <= 0) {
                    onScroll(-moveDistance);
                } else if (tempDistance < 1000) {
                    onScroll(-tempDistance);
                }
            } else if (currentVelocityY < 0 && moveDistance <= 0) {
                if (moveDistance + tempDistance >= 0) {
                    onScroll(-moveDistance);
                } else if (tempDistance < 1000) {
                    onScroll(tempDistance);
                }
            }
            overScrollLogic(tempDistance);
            lastScrollY = currY;

            invalidate();
        }
    }

    /**
     * get Final Over Scroll Distance
     *
     * @return
     */
    private int getFinalOverScrollDistance() {
        return (int) (Math.pow((scroller.getFinalY() - scroller.getCurrY()) * adjustTwinkDuring, 0.4));
    }

    /**
     * scroll over logic
     *
     * @param tempDistance scroll distance
     */
    private void overScrollLogic(int tempDistance) {
        if (overScrollState == 1) {
            startScrollAnimation(tempDistance);
        } else if (overScrollState == 2) {
            startScrollAnimation(-tempDistance);
        }
    }

    /**
     * dell over scroll to move children
     */
    private void startScrollAnimation(final int distanceMove) {
        cancelCurrentAnimation();
        if (scrollAnimation == null) {
            scrollAnimation = ValueAnimator.ofInt(distanceMove, 0);
            scrollAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    onScroll((Integer) animation.getAnimatedValue() * overScrollDampingRatio);
                }
            });
            scrollAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    handleAction();
                }
            });
//            scrollAnimation.setInterpolator(new DecelerateInterpolator(1f));
        } else {
            scrollAnimation.setIntValues(distanceMove, 0);
        }
        scrollAnimation.setDuration(getAnimationTime(getFinalOverScrollDistance()));
        scroller.abortAnimation();
        overScrollState = 0;

        currentAnimation = scrollAnimation;
        scrollAnimation.start();
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     */
    public boolean canChildScrollUp() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
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
     * @return Whether it is possible for the child view of this layout to
     * scroll down. Override this if the child view is a custom view.
     */
    public boolean canChildScrollDown() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (targetView instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) targetView;
                if (absListView.getChildCount() > 0) {
                    int lastChildBottom = absListView.getChildAt(absListView.getChildCount() - 1).getBottom();
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

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (headerView != null) {
            headerHeight = headerView.getMeasuredHeight();
        }
        if (footerView != null) {
            footerHeight = footerView.getMeasuredHeight();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (headerView != null) {
            headerView.layout(left, (int) (-headerHeight), right, 0);
        }
        Log.e("onLayout: ", "" + headerHeight);
        if (footerView != null) {
            footerView.layout(left, bottom - top, right, (int) (bottom - top + footerHeight));
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return !(!pullRefreshEnable && !pullLoadMoreEnable) && super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        isOverScrollTrigger = false;
        cancelCurrentAnimation();
        overScrollState = 0;
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
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if ((ev.getAction() == MotionEvent.ACTION_CANCEL
                || ev.getAction() == MotionEvent.ACTION_UP)
                && moveDistance != 0) {
            handleAction();
        }
        return super.dispatchTouchEvent(ev);
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
        dyUnconsumed = (int) (dyUnconsumed * dragDampingRatio);
        onScroll(-dyUnconsumed);
    }

    @Override
    public int getNestedScrollAxes() {
        return parentHelper.getNestedScrollAxes();
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        if (pullTwinkEnable || autoLoadingEnable) {
            currentVelocityY = velocityY;
            scroller.fling(0, 0, 0, (int) Math.abs(currentVelocityY), 0, 0, 0, Integer.MAX_VALUE);
            lastScrollY = 0;
            invalidate();
        }
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
        if (pullFlowHeight != -1) {
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

        if (!pullTwinkEnable && isRefreshing
                && ((refreshState == 1 && moveDistance < 0)
                || (refreshState == 2 && moveDistance > 0))) {
            moveDistance = 0;
        }

        if ((pullLoadMoreEnable && moveDistance <= 0)
                || (pullRefreshEnable && moveDistance >= 0)
                || pullTwinkEnable) {
            moveChildren(moveDistance);
        } else {
            moveDistance = 0;
            return;
        }

        if (moveDistance >= 0) {
            if (headerView != null && headerView instanceof OnPullListener) {
                ((OnPullListener) headerView).onPullChange(moveDistance / headerHeight);
            }
            if (moveDistance >= headerHeight) {
                if (pullStateControl) {
                    pullStateControl = false;
                    if (headerView != null && !isRefreshing && headerView instanceof OnPullListener) {
                        ((OnPullListener) headerView).onPullHoldTrigger();
                    }
                }
            } else {
                if (!pullStateControl) {
                    pullStateControl = true;
                    if (headerView != null && !isRefreshing && headerView instanceof OnPullListener) {
                        ((OnPullListener) headerView).onPullHoldUnTrigger();
                    }
                }
            }
        } else {
            if (footerView != null && footerView instanceof OnPullListener) {
                ((OnPullListener) footerView).onPullChange(moveDistance / footerHeight);
            }
            if (moveDistance <= -footerHeight) {
                if (pullStateControl) {
                    pullStateControl = false;
                    if (footerView != null && !isRefreshing && footerView instanceof OnPullListener) {
                        ((OnPullListener) footerView).onPullHoldTrigger();
                    }
                }
            } else {
                if (!pullStateControl) {
                    pullStateControl = true;
                    if (footerView != null && !isRefreshing && footerView instanceof OnPullListener) {
                        ((OnPullListener) footerView).onPullHoldUnTrigger();
                    }
                }
            }
        }
    }

    /**
     * move children
     */
    private void moveChildren(float distance) {
        if (moveDistance < 0) {
            if (autoLoadingEnable && !isRefreshing && onRefreshListener != null && !autoLoadTrigger) {
                autoLoadTrigger = true;
                onRefreshListener.onLoading();
            }
        }

        if (headerView != null) {
            headerView.setTranslationY(distance);
        }
        if (footerView != null) {
            footerView.setTranslationY(distance);
        }
        targetView.setTranslationY(distance);
    }

    /**
     * decide on the action refresh or loadMore
     */
    private void handleAction() {
        if (pullRefreshEnable && refreshState != 2
                && !isResetTrigger && moveDistance >= headerHeight) {
            startRefresh(moveDistance);
        } else if ((!isRefreshing && moveDistance > 0 && refreshState != 2)
                || (isResetTrigger && refreshState == 1)
                || moveDistance > 0 && refreshState == 2) {
            resetHeaderView(moveDistance);
        }
        if (pullLoadMoreEnable && refreshState != 1
                && !isResetTrigger && moveDistance <= -footerHeight) {
            startLoadMore(moveDistance);
        } else if ((!isRefreshing && moveDistance < 0 && refreshState != 1)
                || (isResetTrigger && refreshState == 2)
                || moveDistance < 0 && refreshState == 1) {
            resetFootView(moveDistance);
        }
    }

    /**
     * start Refresh
     *
     * @param headerViewHeight
     */
    private void startRefresh(int headerViewHeight) {
        if (headerView != null && headerView instanceof OnPullListener) {
            ((OnPullListener) headerView).onPullHolding();
        }
        ValueAnimator animator = ValueAnimator.ofInt(headerViewHeight, (int) headerHeight);
        cancelCurrentAnimation();
        currentAnimation = animator;
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                moveDistance = (Integer) animation.getAnimatedValue();
                if (headerView != null && headerView instanceof OnPullListener) {
                    ((OnPullListener) headerView).onPullChange(moveDistance / headerHeight);
                }
                moveChildren(moveDistance);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                refreshState = 1;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (onRefreshListener != null && !isRefreshing) {
                    onRefreshListener.onRefresh();
                    isRefreshing = true;

                    if (footerView != null) {
                        footerView.setVisibility(GONE);
                    }
                }
            }
        });
        if (headerViewHeight == 0) {
            animator.setDuration(refreshBackTime);
        } else {
            animator.setDuration(getAnimationTime(moveDistance));
        }
        animator.setInterpolator(new DecelerateInterpolator(2f));
        animator.start();
    }

    /**
     * reset refresh refreshState
     *
     * @param headerViewHeight
     */
    private void resetHeaderView(int headerViewHeight) {
        if (headerViewHeight == 0 && refreshState == 1) {
            resetRefreshState();
            return;
        }
        ValueAnimator animator = ValueAnimator.ofInt(headerViewHeight, 0);
        cancelCurrentAnimation();
        currentAnimation = animator;
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                moveDistance = (Integer) animation.getAnimatedValue();
                if (headerView != null && headerView instanceof OnPullListener) {
                    ((OnPullListener) headerView).onPullChange(moveDistance / headerHeight);
                }
                moveChildren(moveDistance);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (headerView != null && isRefreshing && refreshState == 1 && headerView instanceof OnPullListener) {
                    ((OnPullListener) headerView).onPullFinish();
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (refreshState == 1) {
                    resetRefreshState();
                }
            }
        });
        if (refreshBackTime != -1) {
            animator.setDuration(refreshBackTime);
        } else {
            animator.setDuration(getAnimationTime(moveDistance));
        }
        animator.start();
    }

    private void resetRefreshState() {
        if (headerView != null && headerView instanceof OnPullListener) {
            ((OnPullListener) headerView).onPullReset();
        }
        if (moveDistance != 0) {
            return;
        }
        if (footerView != null) {
            footerView.setVisibility(VISIBLE);
        }
        if (headerView != null) {
            headerView.setVisibility(VISIBLE);
        }
        isRefreshing = false;
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
        if (footerView != null && footerView instanceof OnPullListener) {
            ((OnPullListener) footerView).onPullHolding();
        }
        ValueAnimator animator = ValueAnimator.ofInt(loadMoreViewHeight, -(int) footerHeight);
        cancelCurrentAnimation();
        currentAnimation = animator;
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                moveDistance = (Integer) animation.getAnimatedValue();
                if (headerView != null && headerView instanceof OnPullListener) {
                    ((OnPullListener) headerView).onPullChange(moveDistance / footerHeight);
                }
                moveChildren(moveDistance);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                refreshState = 2;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (onRefreshListener != null && !isRefreshing) {
                    onRefreshListener.onLoading();
                    isRefreshing = true;

                    if (headerView != null) {
                        headerView.setVisibility(GONE);
                    }
                }
            }
        });
        animator.setDuration(getAnimationTime(moveDistance));
        animator.setInterpolator(new DecelerateInterpolator(2f));
        animator.start();
    }

    private void cancelCurrentAnimation() {
        if (currentAnimation != null && currentAnimation.isRunning()) {
            currentAnimation.cancel();
        }
    }

    /**
     * reset loadMore refreshState
     *
     * @param loadMoreViewHeight
     */
    private void resetFootView(int loadMoreViewHeight) {
        if (loadMoreViewHeight == 0 && refreshState == 2) {
            resetLoadMoreState();
            return;
        }
        ValueAnimator animator = ValueAnimator.ofInt(loadMoreViewHeight, 0);
        cancelCurrentAnimation();
        currentAnimation = animator;
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                moveDistance = (Integer) animation.getAnimatedValue();
                if (footerView != null && footerView instanceof OnPullListener) {
                    ((OnPullListener) footerView).onPullChange(moveDistance / footerHeight);
                }
                moveChildren(moveDistance);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (refreshState == 2) {
                    resetLoadMoreState();
                }
            }

            @Override
            public void onAnimationStart(Animator animation) {
                if (footerView != null && isRefreshing && refreshState == 2 && footerView instanceof OnPullListener) {
                    ((OnPullListener) footerView).onPullFinish();
                }
            }
        });
        if (refreshBackTime != -1) {
            animator.setDuration(refreshBackTime);
        } else {
            animator.setDuration(getAnimationTime(moveDistance));
        }
        animator.start();
    }

    private long getAnimationTime(int moveDistance) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        float ratio = Math.abs((float) moveDistance / (float) displayMetrics.heightPixels);
        return (long) (Math.pow(2000 * ratio, 0.5) * duringAdjustValue);
    }

    private void resetLoadMoreState() {
        if (footerView != null && footerView instanceof OnPullListener) {
            ((OnPullListener) footerView).onPullReset();
        }
        if (moveDistance != 0) {
            return;
        }
        if (footerView != null) {
            footerView.setVisibility(VISIBLE);
        }
        if (headerView != null) {
            headerView.setVisibility(VISIBLE);
        }
        isRefreshing = false;
        refreshState = 0;
        isResetTrigger = false;
        pullStateControl = true;
    }

    public void autoRefresh() {
        if (targetView == null || !pullRefreshEnable) {
            return;
        }
        startRefresh(0);
    }

    /**
     * callback on refresh finish
     */
    public void refreshComplete() {
        if (refreshState == 1) {
            isResetTrigger = true;
            resetHeaderView(moveDistance);
        }
    }

    /**
     * Callback on loadMore finish
     */
    public void loadMoreComplete() {
        if (refreshState == 2) {
            isResetTrigger = true;
            resetFootView(moveDistance);
        }
        autoLoadTrigger = false;
    }

    private float dipToPx(Context context, float value) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, metrics);
    }

    public void setHeaderView(View header) {
        headerView = header;
    }

    public void setFooterView(View footer) {
        footerView = footer;
    }

    public void setLoadMoreEnable(boolean mPullLoadEnable) {
        this.pullLoadMoreEnable = mPullLoadEnable;
    }

    public void setRefreshEnable(boolean mPullRefreshEnable) {
        this.pullRefreshEnable = mPullRefreshEnable;
    }

    public void setPullTwinkEnable(boolean pullTwinkEnable) {
        this.pullTwinkEnable = pullTwinkEnable;
    }

    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        this.onRefreshListener = onRefreshListener;
    }

    public void setOverScrollDampingRatio(float overScrollDampingRatio) {
        this.overScrollDampingRatio = overScrollDampingRatio;
    }

    private void setScrollInterpolator(Interpolator interpolator) {
        scroller = ScrollerCompat.create(getContext(), interpolator);
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

    public void setAdjustTwinkDuring(int adjustTwinkDuring) {
        this.adjustTwinkDuring = adjustTwinkDuring;
    }

    public void setAutoLoadingEnable(boolean ableAutoLoading) {
        autoLoadingEnable = ableAutoLoading;
    }

    public int getRefreshState() {
        return refreshState;
    }

    public boolean isLoadMoreEnable() {
        return pullLoadMoreEnable;
    }

    public boolean isRefreshEnable() {
        return pullRefreshEnable;
    }

    public boolean isRefreshing() {
        return isRefreshing;
    }

    public interface OnPullListener {
        void onPullChange(float percent);

        void onPullReset();

        void onPullHoldTrigger();

        void onPullHoldUnTrigger();

        void onPullHolding();

        void onPullFinish();
    }

    public static class OnRefreshListener {
        public void onRefresh() {
        }

        public void onLoading() {
        }
    }
}