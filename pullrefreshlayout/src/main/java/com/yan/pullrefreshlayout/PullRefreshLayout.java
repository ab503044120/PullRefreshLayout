package com.yan.pullrefreshlayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ScrollerCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.AbsListView;
import android.widget.FrameLayout;

/**
 * Created by yan on 2017/4/11.
 */
public class PullRefreshLayout extends ViewGroup implements NestedScrollingParent,
        NestedScrollingChild {
    private final NestedScrollingParentHelper parentHelper;
    private final NestedScrollingChildHelper childHelper;
    private final int[] parentScrollConsumed = new int[2];
    private final int[] parentOffsetInWindow = new int[2];

    /**
     * refresh header layout
     */
    final PullFrameLayout headerViewLayout;

    /**
     * refresh footer layout
     */
    final PullFrameLayout footerViewLayout;

    /**
     * refresh header
     */
    View headerView;

    /**
     * refresh footer
     */
    View footerView;

    /**
     * refresh target view
     */
    View targetView;

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
    private int adjustTwinkDuring = 3;

    /**
     * over scroll state
     */
    private int overScrollState = 0;

    /**
     * drag move distance
     */
    volatile int moveDistance = 0;

    /**
     * final motion event
     */
    private MotionEvent[] finalMotionEvent = new MotionEvent[1];

    /**
     * header height
     */
    private float refreshTriggerDistance = 60;

    /**
     * footer trigger distance
     */
    private float loadTriggerDistance = 60;

    /**
     * max height drag
     */
    private float pullLimitDistance = -1;

    /**
     * the ratio for final distance for drag
     */
    private float dragDampingRatio = 0.6f;

    /**
     * move distance ratio for over scroll
     */
    private float overScrollDampingRatio = 0.2f;

    /**
     * animation during adjust value
     */
    private float duringAdjustValue = 10f;

    /**
     * current velocity y
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
     * is header height set
     */
    private boolean isHeaderHeightSet = false;

    /**
     * is footer height set
     */
    private boolean isFooterHeightSet = false;

    /**
     * refresh back time
     * if the value equals -1, the field duringAdjustValue will be work
     */
    private long refreshBackTime = 350;

    private final RefreshShowHelper refreshShowHelper;

    private final GeneralPullHelper generalPullHelper;

    private OnRefreshListener onRefreshListener;

    private OnDragIntercept onDragIntercept;

    private ValueAnimator currentAnimation;

    private ValueAnimator scrollAnimation;

    private ScrollerCompat scroller;

    public PullRefreshLayout(Context context) {
        this(context, null, 0);
    }

    public PullRefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        refreshShowHelper = new RefreshShowHelper(this);
        generalPullHelper = new GeneralPullHelper(this);

        parentHelper = new NestedScrollingParentHelper(this);
        childHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);

        this.headerViewLayout = new PullFrameLayout(context);
        this.footerViewLayout = new PullFrameLayout(context);

        refreshTriggerDistance = dipToPx(context, refreshTriggerDistance);
        loadTriggerDistance = dipToPx(context, loadTriggerDistance);

        addView(headerViewLayout, new LayoutParams(-1, -2));
        addView(footerViewLayout, new LayoutParams(-1, -2));
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() <= 2) {
            throw new RuntimeException("PullRefreshLayout should have one child");
        }
        if (targetView == null) {
            targetView = getPullContentView();
        }

    }

    private void readyScroller() {
        if ((pullTwinkEnable || autoLoadingEnable) && scroller == null) {
            if (targetView instanceof RecyclerView) {
                scroller = ScrollerCompat.create(getContext(), getRecyclerDefaultInterpolator());
                return;
            }
            scroller = ScrollerCompat.create(getContext());
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
            if (!isOverScrollTrigger && !canChildScrollUp() && canChildScrollDown() && currentVelocityY < 0) {
                isOverScrollTrigger = true;
                onOverScrollUp();
            } else if (!isOverScrollTrigger && !canChildScrollDown() && canChildScrollUp() && currentVelocityY > 0) {
                isOverScrollTrigger = true;
                onOverScrollDown();
            }

            int currY = scroller.getCurrY();
            int tempDistance = currY - lastScrollY;
            if (currentVelocityY > 0 && moveDistance >= 0 && getPullContentView() instanceof NestedScrollingChild) {
                if (moveDistance - tempDistance <= 0) {
                    onScroll(-moveDistance);
                } else if (tempDistance < 1000) {
                    onScroll(tempDistance < 1000 ? -tempDistance : 0);
                }
            } else if (currentVelocityY < 0 && moveDistance <= 0 && getPullContentView() instanceof NestedScrollingChild) {
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
        overScrollState = 0;
        int finalDistance = getFinalOverScrollDistance();
        cancelCurrentAnimation();

        if (scrollAnimation == null) {
            scrollAnimation = ValueAnimator.ofInt(distanceMove, 0);
            scrollAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    onNestedScroll(getPullContentView(), 0, 0, 0, (int) (-(Integer) animation.getAnimatedValue() * overScrollDampingRatio));
                }
            });
            scrollAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    onNestedScrollAccepted(getPullContentView(), getPullContentView(), 2);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    handleAction();
                    onStopNestedScroll(getPullContentView());
                }
            });
//            scrollAnimation.setInterpolator(new DecelerateInterpolator(1f));
        } else {
            scrollAnimation.setIntValues(distanceMove, 0);
        }
        scrollAnimation.setDuration(getAnimationTime(finalDistance));

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
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        if (headerView != null && !isHeaderHeightSet) {
            headerView.measure(widthMeasureSpec, 0);
            refreshTriggerDistance = headerView.getMeasuredHeight();
        }
        if (footerView != null && !isFooterHeightSet) {
            footerView.measure(widthMeasureSpec, 0);
            loadTriggerDistance = footerView.getMeasuredHeight();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        getPullContentView().layout(0, 0, getMeasuredWidth(), getMeasuredHeight());
        headerViewLayout.layout(0, 0, getMeasuredWidth(), moveDistance);
        footerViewLayout.layout(0, getMeasuredHeight() + moveDistance, getMeasuredWidth(), getMeasuredHeight());
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        parentHelper.onNestedScrollAccepted(child, target, axes);
        startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
    }

    /**
     * handler : refresh or loading
     *
     * @param child : child view of PullRefreshLayout,RecyclerView or Scroller
     */
    @Override
    public void onStopNestedScroll(View child) {
        parentHelper.onStopNestedScroll(child);
        stopNestedScroll();
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
                consumed[1] += dy;
                return;
            }
            onScroll(-dy);
            consumed[1] += dy;
        }
        if (dy < 0 && moveDistance < 0) {
            if (moveDistance - dy > 0) {
                onScroll(-moveDistance);
                consumed[1] += dy;
                return;
            }
            onScroll(-dy);
            consumed[1] += dy;
        }

        final int[] parentConsumed = parentScrollConsumed;
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0];
            consumed[1] += parentConsumed[1];
        }
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                parentOffsetInWindow);
        int dy = dyUnconsumed + parentOffsetInWindow[1];
        dy = (int) (dy * dragDampingRatio);
        onScroll(-dy);
    }

    @Override
    public int getNestedScrollAxes() {
        return parentHelper.getNestedScrollAxes();
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        boolean nestedPreFling = dispatchNestedPreFling(velocityX, velocityY);
        if (nestedPreFling) {
            return true;
        }
        if (pullTwinkEnable || autoLoadingEnable) {
            currentVelocityY = velocityY;
            readyScroller();
            scroller.fling(0, 0, 0, (int) Math.abs(currentVelocityY), 0, 0, 0, Integer.MAX_VALUE);
            lastScrollY = 0;
            invalidate();
        }
        return false;
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        return dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        childHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return childHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return childHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        childHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return childHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {
        return childHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return childHelper.dispatchNestedPreScroll(
                dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return childHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return childHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (getPullContentView() instanceof NestedScrollingChild) {
            return !(!pullRefreshEnable && !pullLoadMoreEnable) && super.onInterceptTouchEvent(ev);
        }
        return !generalPullHelper.onInterceptTouchEvent(ev) && super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getPullContentView() instanceof NestedScrollingChild) {
            return super.onTouchEvent(event);
        }
        return generalPullHelper.onTouchEvent(event);
    }

    private void actionEndHandleAction(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            isOverScrollTrigger = false;
            cancelCurrentAnimation();
            overScrollState = 0;
        }

        if ((ev.getAction() == MotionEvent.ACTION_CANCEL
                || ev.getAction() == MotionEvent.ACTION_UP)
                && moveDistance != 0) {
            handleAction();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        actionEndHandleAction(ev);
        generalPullHelper.dellDirection(ev);
        if (!(getPullContentView() instanceof NestedScrollingChild)) {
            return !generalPullHelper.dispatchTouchEvent(ev, finalMotionEvent) && super.dispatchTouchEvent(finalMotionEvent[0]);
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * dell the nestedScroll
     *
     * @param distanceY move distance of Y
     */
    private void onScroll(float distanceY) {
        if (checkMoving(distanceY)) {
            return;
        }
        moveDistance += distanceY;
        if (pullLimitDistance != -1) {
            moveDistance = (int) Math.min(moveDistance, pullLimitDistance);
            moveDistance = (int) Math.max(moveDistance, -pullLimitDistance);
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
                ((OnPullListener) headerView).onPullChange(
                        refreshShowHelper.headerOffsetRatio(moveDistance / refreshTriggerDistance)
                );
            }
            if (moveDistance >= refreshTriggerDistance) {
                if (pullStateControl) {
                    pullStateControl = false;
                    if (headerView != null && !isRefreshing && headerView instanceof OnPullListener) {
                        ((OnPullListener) headerView).onPullHoldTrigger();
                    }
                }
                return;
            }
            if (!pullStateControl) {
                pullStateControl = true;
                if (headerView != null && !isRefreshing && headerView instanceof OnPullListener) {
                    ((OnPullListener) headerView).onPullHoldUnTrigger();
                }
            }
            return;
        }
        if (footerView != null && footerView instanceof OnPullListener) {
            ((OnPullListener) footerView).onPullChange(
                    refreshShowHelper.footerOffsetRatio(moveDistance / loadTriggerDistance)
            );
        }
        if (moveDistance <= -loadTriggerDistance) {
            if (pullStateControl) {
                pullStateControl = false;
                if (footerView != null && !isRefreshing && footerView instanceof OnPullListener) {
                    ((OnPullListener) footerView).onPullHoldTrigger();
                }
            }
            return;
        }
        if (!pullStateControl) {
            pullStateControl = true;
            if (footerView != null && !isRefreshing && footerView instanceof OnPullListener) {
                ((OnPullListener) footerView).onPullHoldUnTrigger();
            }
        }
    }

    private boolean checkMoving(float distanceY) {
        if (((distanceY > 0 && moveDistance == 0) || moveDistance > 0)
                && onDragIntercept != null && !onDragIntercept.onHeaderDownIntercept()) {
            return true;
        } else if (((distanceY < 0 && moveDistance == 0) || moveDistance < 0)
                && onDragIntercept != null && !onDragIntercept.onFooterUpIntercept()) {
            return true;
        }
        return false;
    }

    /**
     * move children
     */
    private void moveChildren(int distance) {
        moveDistance = distance;
        dellAutoLoading();
        ViewCompat.setTranslationY(getPullContentView(), distance);
        dellRefreshViewCenter(distance);
    }

    private void dellAutoLoading() {
        if (moveDistance == 0 && autoLoadingEnable
                && !isRefreshing && onRefreshListener != null
                && !autoLoadTrigger && !canChildScrollDown()) {
            autoLoadTrigger = true;
            onRefreshListener.onLoading();
        }
    }

    /**
     * make sure refresh view center in parent
     *
     * @param distance
     */
    private void dellRefreshViewCenter(float distance) {
        LayoutParams headerLayoutParams = headerViewLayout.getLayoutParams();
        headerLayoutParams.height = (int) distance;
        headerViewLayout.setLayoutParams(headerLayoutParams);

        LayoutParams footerLayoutParams = footerViewLayout.getLayoutParams();
        footerLayoutParams.height = (int) distance;
        footerViewLayout.setLayoutParams(footerLayoutParams);
    }

    /**
     * decide on the action refresh or loadMore
     */
    private void handleAction() {
        if (pullRefreshEnable && refreshState != 2
                && !isResetTrigger && moveDistance >= refreshTriggerDistance) {
            startRefresh(moveDistance, true);
        } else if ((!isRefreshing && moveDistance > 0 && refreshState != 2)
                || (isResetTrigger && refreshState == 1)
                || moveDistance > 0 && refreshState == 2) {
            resetHeaderView(moveDistance);
        }
        if (pullLoadMoreEnable && refreshState != 1
                && !isResetTrigger && moveDistance <= -loadTriggerDistance) {
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
    private void startRefresh(int headerViewHeight, final boolean withAction) {
        if (headerView != null && headerView instanceof OnPullListener) {
            ((OnPullListener) headerView).onPullHolding();
        }
        ValueAnimator animator = ValueAnimator.ofInt(headerViewHeight, (int) refreshTriggerDistance);
        cancelCurrentAnimation();
        currentAnimation = animator;
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                moveChildren((Integer) animation.getAnimatedValue());
                if (headerView != null && headerView instanceof OnPullListener) {
                    ((OnPullListener) headerView).onPullChange(
                            refreshShowHelper.headerOffsetRatio(moveDistance / refreshTriggerDistance)
                    );
                }
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                refreshState = 1;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!isRefreshing) {
                    if (onRefreshListener != null && withAction) {
                        onRefreshListener.onRefresh();
                    }
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
                moveChildren((Integer) animation.getAnimatedValue());
                if (headerView != null && headerView instanceof OnPullListener) {
                    ((OnPullListener) headerView).onPullChange(
                            refreshShowHelper.headerOffsetRatio(moveDistance / refreshTriggerDistance)
                    );
                }
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
        animator.setDuration(refreshBackTime != -1 ? refreshBackTime : getAnimationTime(moveDistance));
        animator.start();
    }

    private void resetRefreshState() {
        if (headerView != null && headerView instanceof OnPullListener) {
            ((OnPullListener) headerView).onPullReset();
        }
        if (footerView != null) {
            footerView.setVisibility(VISIBLE);
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
        ValueAnimator animator = ValueAnimator.ofInt(loadMoreViewHeight, -(int) loadTriggerDistance);
        cancelCurrentAnimation();
        currentAnimation = animator;
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                moveChildren((Integer) animation.getAnimatedValue());
                if (footerView != null && footerView instanceof OnPullListener) {
                    ((OnPullListener) footerView).onPullChange(
                            refreshShowHelper.footerOffsetRatio(moveDistance / refreshTriggerDistance)
                    );
                }
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
                moveChildren((Integer) animation.getAnimatedValue());
                if (footerView != null && footerView instanceof OnPullListener) {
                    ((OnPullListener) footerView).onPullChange(
                            refreshShowHelper.footerOffsetRatio(moveDistance / refreshTriggerDistance)
                    );
                }
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
        animator.setDuration(refreshBackTime != -1 ? refreshBackTime : getAnimationTime(moveDistance));
        animator.start();
    }

    private void resetLoadMoreState() {
        if (footerView != null && footerView instanceof OnPullListener) {
            ((OnPullListener) footerView).onPullReset();
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

    public void autoRefresh() {
        autoRefresh(true);
    }

    public void autoRefresh(boolean withAction) {
        if (getPullContentView() == null || !pullRefreshEnable) {
            return;
        }
        if (!(getPullContentView() instanceof NestedScrollingChild)) {
            generalPullHelper.autoRefreshDell();
        }
        startRefresh(0, withAction);
    }

    private void cancelCurrentAnimation() {
        if (currentAnimation != null && currentAnimation.isRunning()) {
            currentAnimation.cancel();
        }
        if (scroller != null && !scroller.isFinished()) {
            scroller.abortAnimation();
        }
    }

    private long getAnimationTime(int moveDistance) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        float ratio = Math.abs((float) moveDistance / (float) displayMetrics.heightPixels);
        return (long) (Math.pow(2000 * ratio, 0.5) * duringAdjustValue);
    }

    private float dipToPx(Context context, float value) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, metrics);
    }

    private View getPullContentView() {
        return getChildAt(2);
    }

    public void setHeaderView(View header) {
        if (header != null) {
            headerView = header;
            headerViewLayout.removeAllViewsInLayout();
            refreshShowHelper.dellRefreshHeaderShow();
            headerViewLayout.addView(headerView);
        }
    }

    public void setFooterView(View footer) {
        if (footer != null) {
            footerView = footer;
            footerViewLayout.removeAllViewsInLayout();
            refreshShowHelper.dellRefreshFooterShow();
            footerViewLayout.addView(footerView);
        }
    }

    public void setTargetView(View targetView) {
        this.targetView = targetView;
    }

    public void setLoadMoreEnable(boolean loadEnable) {
        this.pullLoadMoreEnable = loadEnable;
    }

    public void setRefreshEnable(boolean refreshEnable) {
        this.pullRefreshEnable = refreshEnable;
    }

    public void setTwinkEnable(boolean twinkEnable) {
        this.pullTwinkEnable = twinkEnable;
    }

    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        this.onRefreshListener = onRefreshListener;
    }

    public void setOnDragIntercept(OnDragIntercept onDragIntercept) {
        this.onDragIntercept = onDragIntercept;
    }

    public void setOverScrollDampingRatio(float overScrollDampingRatio) {
        this.overScrollDampingRatio = overScrollDampingRatio;
    }

    private void setScrollInterpolator(Interpolator interpolator) {
        scroller = ScrollerCompat.create(getContext(), interpolator);
    }

    public void setRefreshTriggerDistance(float refreshTriggerDistance) {
        isHeaderHeightSet = true;
        this.refreshTriggerDistance = refreshTriggerDistance;
    }

    public void setLoadTriggerDistance(float loadTriggerDistance) {
        isFooterHeightSet = true;
        this.loadTriggerDistance = loadTriggerDistance;
    }

    public void setPullLimitDistance(float pullLimitDistance) {
        this.pullLimitDistance = pullLimitDistance;
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

    public void setRefreshShowGravity(@RefreshShowHelper.ShowState int headerShowGravity
            , @RefreshShowHelper.ShowState int footerShowGravity) {
        refreshShowHelper.setHeaderShowGravity(headerShowGravity);
        refreshShowHelper.setFooterShowGravity(footerShowGravity);
    }

    public void setHeaderShowGravity(@RefreshShowHelper.ShowState int headerShowGravity) {
        refreshShowHelper.setHeaderShowGravity(headerShowGravity);
    }

    public void setFooterShowGravity(@RefreshShowHelper.ShowState int footerShowGravity) {
        refreshShowHelper.setFooterShowGravity(footerShowGravity);
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

    public boolean isTwinkEnable() {
        return pullTwinkEnable;
    }

    public boolean isRefreshing() {
        return isRefreshing;
    }

    public boolean isLayoutMoving() {
        return moveDistance != 0;
    }

    public boolean isDragDown() {
        return generalPullHelper.dragState == 1;
    }

    public boolean isDragUp() {
        return generalPullHelper.dragState == -1;
    }

    public boolean isMovingDirectDown() {
        return generalPullHelper.isMovingDirectDown;
    }

    public interface OnPullListener {
        void onPullChange(float percent);

        void onPullHoldTrigger();

        void onPullHoldUnTrigger();

        void onPullHolding();

        void onPullFinish();

        void onPullReset();

    }

    public interface OnDragIntercept {
        boolean onHeaderDownIntercept();

        boolean onFooterUpIntercept();
    }

    public static class OnDragInterceptAdapter implements OnDragIntercept {
        public boolean onHeaderDownIntercept() {
            return true;
        }

        public boolean onFooterUpIntercept() {
            return true;
        }
    }

    public interface OnRefreshListener {
        void onRefresh();

        void onLoading();
    }

    public static class OnRefreshListenerAdapter implements OnRefreshListener {
        public void onRefresh() {
        }

        public void onLoading() {
        }
    }

    private class PullFrameLayout extends FrameLayout {
        public PullFrameLayout(@NonNull Context context) {
            super(context);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return event.getActionMasked() == MotionEvent.ACTION_DOWN && getPullContentView() != null
                    && !(getPullContentView() instanceof NestedScrollingChild) || super.onTouchEvent(event);
        }
    }

}