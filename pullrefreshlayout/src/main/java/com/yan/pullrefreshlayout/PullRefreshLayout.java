package com.yan.pullrefreshlayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.ScrollerCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import java.lang.reflect.Constructor;

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

    private View pullContentView;

    /**
     * current refreshing state 1:refresh 2:loadMore
     */
    private int refreshState = 0;

    /**
     * last Scroll Y
     */
    private int lastScrollY = 0;
    private int scrollTempOffset = 0;

    /**
     * over scroll state
     */
    private int overScrollState = 0;

    /**
     * drag move distance
     */
    volatile int moveDistance = 0;

    /**
     * trigger distance
     */
    int refreshTriggerDistance = 60;
    int loadTriggerDistance = 60;

    /**
     * over scroll start offset
     */
    private int overScrollMaxTriggerOffset = 80;

    /**
     * max drag distance
     */
    private int pullLimitDistance = -1;

    /**
     * the ratio for final distance for drag
     */
    private float dragDampingRatio = 0.6f;

    /**
     * animation during adjust value
     */
    private float duringAdjustValue = 10f;

    /**
     * move distance ratio for over scroll
     */
    private float overScrollDampingRatio = 0.2f;

    /**
     * twink during adjust value
     */
    private float twinkDuringAdjustValue = 3;

    /**
     * final scroll distance
     */
    private float finalScrollDistance = -1;

    /**
     * switch refresh enable
     */
    private boolean pullRefreshEnable = true;

    /**
     * switch Twink enable
     */
    private boolean pullTwinkEnable = true;

    /**
     * switch loadMore enable
     */
    private boolean pullLoadMoreEnable = false;

    /**
     * switch able auto load more
     */
    private boolean autoLoadingEnable = false;

    /**
     * make sure header or footer hold trigger one time
     */
    private boolean pullStateControl = true;

    /**
     * refreshing state trigger
     */
    private boolean isAutoRefreshTrigger = false;
    private boolean isHoldingTrigger = false;
    private boolean isHoldingFinishTrigger = false;
    private boolean isResetTrigger = false;
    private boolean isAutoLoadTrigger = false;
    private boolean isOverScrollTrigger = false;

    /**
     * is header or footer height set
     */
    private boolean isHeaderHeightSet = false;
    private boolean isFooterHeightSet = false;

    /**
     * refresh with action
     */
    private boolean refreshWithAction = true;

    /**
     * refresh back time
     * if the value equals -1, the field duringAdjustValue will be work
     */
    private int refreshBackTime = 350;

    /**
     * final motion event
     */
    private MotionEvent[] finalMotionEvent = new MotionEvent[1];

    private final RefreshShowHelper refreshShowHelper;
    private final GeneralPullHelper generalPullHelper;

    private OnRefreshListener onRefreshListener;
    private OnDragIntercept onDragIntercept;

    private ScrollerCompat scroller;

    private ValueAnimator startRefreshAnimator;
    private ValueAnimator resetHeaderAnimator;
    private ValueAnimator startLoadMoreAnimator;
    private ValueAnimator resetFootAnimator;
    private ValueAnimator scrollAnimation;

    public PullRefreshLayout(Context context) {
        this(context, null, 0);
    }

    public PullRefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        refreshShowHelper = new RefreshShowHelper(this);
        generalPullHelper = new GeneralPullHelper(this, context);

        parentHelper = new NestedScrollingParentHelper(this);
        childHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
        loadAttribute(context, attrs);

        setHeaderView(headerView);
        setFooterView(footerView);
    }

    private void loadAttribute(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PullRefreshLayout);
        pullRefreshEnable = typedArray.getBoolean(R.styleable.PullRefreshLayout_prl_refreshEnable, pullRefreshEnable);
        pullLoadMoreEnable = typedArray.getBoolean(R.styleable.PullRefreshLayout_prl_loadMoreEnable, pullLoadMoreEnable);
        pullTwinkEnable = typedArray.getBoolean(R.styleable.PullRefreshLayout_prl_twinkEnable, pullTwinkEnable);
        autoLoadingEnable = typedArray.getBoolean(R.styleable.PullRefreshLayout_prl_autoLoadingEnable, autoLoadingEnable);

        refreshTriggerDistance = dipToPx(context, refreshTriggerDistance);
        loadTriggerDistance = dipToPx(context, loadTriggerDistance);
        if (typedArray.hasValue(R.styleable.PullRefreshLayout_prl_refreshTriggerDistance)) {
            isHeaderHeightSet = true;
            refreshTriggerDistance = typedArray.getDimensionPixelOffset(R.styleable.PullRefreshLayout_prl_refreshTriggerDistance, refreshTriggerDistance);
        }
        if (typedArray.hasValue(R.styleable.PullRefreshLayout_prl_loadTriggerDistance)) {
            isFooterHeightSet = true;
            loadTriggerDistance = typedArray.getDimensionPixelOffset(R.styleable.PullRefreshLayout_prl_loadTriggerDistance, loadTriggerDistance);
        }
        pullLimitDistance = typedArray.getDimensionPixelOffset(R.styleable.PullRefreshLayout_prl_pullLimitDistance, pullLimitDistance);
        overScrollMaxTriggerOffset = typedArray.getDimensionPixelOffset(R.styleable.PullRefreshLayout_prl_overScrollMaxTriggerOffset, dipToPx(context, overScrollMaxTriggerOffset));

        dragDampingRatio = typedArray.getFloat(R.styleable.PullRefreshLayout_prl_dragDampingRatio, dragDampingRatio);
        duringAdjustValue = typedArray.getFloat(R.styleable.PullRefreshLayout_prl_duringAdjustValue, duringAdjustValue);
        overScrollDampingRatio = typedArray.getFloat(R.styleable.PullRefreshLayout_prl_overScrollDampingRatio, overScrollDampingRatio);
        twinkDuringAdjustValue = typedArray.getFloat(R.styleable.PullRefreshLayout_prl_twinkDuringAdjustValue, twinkDuringAdjustValue);

        refreshBackTime = typedArray.getInteger(R.styleable.PullRefreshLayout_prl_refreshBackTime, refreshBackTime);

        if (typedArray.hasValue(R.styleable.PullRefreshLayout_prl_headerShowGravity)) {
            refreshShowHelper.setHeaderShowGravity(typedArray.getInteger(R.styleable.PullRefreshLayout_prl_headerShowGravity, RefreshShowHelper.STATE_FOLLOW));
        }
        if (typedArray.hasValue(R.styleable.PullRefreshLayout_prl_footerShowGravity)) {
            refreshShowHelper.setFooterShowGravity(typedArray.getInteger(R.styleable.PullRefreshLayout_prl_footerShowGravity, RefreshShowHelper.STATE_FOLLOW));
        }
        initHeaderOrFooter(context, typedArray);

        typedArray.recycle();
    }

    @Nullable
    private View parseClassName(Context context, String className) {
        if (!TextUtils.isEmpty(className)) {
            try {
                final Class<?>[] CONSTRUCTOR_PARAMS = new Class<?>[]{Context.class};
                final Class<View> clazz = (Class<View>) Class.forName(className, true, context.getClassLoader());
                Constructor<View> constructor = clazz.getConstructor(CONSTRUCTOR_PARAMS);
                constructor.setAccessible(true);
                return constructor.newInstance(context);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void initHeaderOrFooter(Context context, TypedArray typedArray) {
        headerView = parseClassName(context, typedArray.getString(R.styleable.PullRefreshLayout_prl_headerClass));
        if (headerView == null) {
            int headerViewId = typedArray.getResourceId(R.styleable.PullRefreshLayout_prl_headerViewId, View.NO_ID);
            if (headerViewId != View.NO_ID) {
                headerView = LayoutInflater.from(context).inflate(headerViewId, null, false);
            }
        }
        footerView = parseClassName(context, typedArray.getString(R.styleable.PullRefreshLayout_prl_footerClass));
        if (footerView == null) {
            int footerViewId = typedArray.getResourceId(R.styleable.PullRefreshLayout_prl_footerViewId, View.NO_ID);
            if (footerViewId != View.NO_ID) {
                footerView = LayoutInflater.from(context).inflate(footerViewId, null, false);
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        targetView = getPullContentView();
        if ((pullTwinkEnable || autoLoadingEnable)) {
            readyScroller();
        }
    }

    private View getPullContentView() {
        if (pullContentView != null) {
            return pullContentView;
        }

        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i) != footerView && getChildAt(i) != headerView) {
                return pullContentView = getChildAt(i);
            }
        }
        throw new RuntimeException("PullRefreshLayout should have one child");
    }

    private void readyScroller() {
        if ((pullTwinkEnable || autoLoadingEnable) && scroller == null) {
            if (targetView instanceof RecyclerView) {
                scroller = ScrollerCompat.create(getContext(), getRecyclerDefaultInterpolator());
                ((RecyclerView) targetView).addOnScrollListener(getRecyclerScrollListener());
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

    private void onOverScrollUp() {
        overScrollState = 1;
    }

    private void onOverScrollDown() {
        overScrollState = 2;
        if (autoLoadingEnable && refreshState == 0 && !isAutoLoadTrigger && onRefreshListener != null) {
            isAutoLoadTrigger = true;
            onRefreshListener.onLoading();
        }
    }

    /**
     * overScroll Back Dell
     *
     * @param type         just to make sure the condition
     * @param tempDistance temp move distance
     * @return need continue
     */
    private boolean overScrollBackDell(int type, int tempDistance) {
        if ((type == 1 && moveDistance - tempDistance <= 0)
                || (type == 2 && moveDistance - tempDistance >= 0)) {
            onScroll(-moveDistance);
            if (!(pullContentView instanceof NestedScrollingChild)) {
                overScrollState = 1;
                overScrollLogic(tempDistance);
            }
            return true;
        }
        if ((type == 1 && (finalScrollDistance > moveDistance))
                || (type == 2 && finalScrollDistance < moveDistance)) {
            cancelAllAnimation();
            onScroll(-tempDistance);
            return false;
        }
        abortScroller();
        handleAction();
        return true;
    }

    private RecyclerView.OnScrollListener getRecyclerScrollListener() {
        return new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (ScrollingUtil.isRecyclerViewToTop(recyclerView) && generalPullHelper.isMovingDirectDown) {
                    onOverScrollUp();
                    overScrollLogic(scrollTempOffset);
                } else if (ScrollingUtil.isRecyclerViewToBottom(recyclerView) && !generalPullHelper.isMovingDirectDown) {
                    onOverScrollDown();
                    overScrollLogic(scrollTempOffset);
                }
            }
        };
    }

    @Override
    public void computeScroll() {
        if (scroller != null && scroller.computeScrollOffset()) {
            int currY = scroller.getCurrY();
            scrollTempOffset = currY - lastScrollY;
            lastScrollY = currY;
            if ((scrollTempOffset > 0 && moveDistance > 0 && overScrollBackDell(1, scrollTempOffset))
                    || (scrollTempOffset < 0 && moveDistance < 0 && overScrollBackDell(2, scrollTempOffset))) {
                return;
            } else if ((scrollTempOffset < 0 && moveDistance > 0) || (scrollTempOffset > 0 && moveDistance < 0)) {
                cancelAllAnimation();
                handleAction();
                abortScroller();
                return;
            }

            invalidate();

            if (targetView instanceof RecyclerView) {
                return;
            }

            if (!isOverScrollTrigger && !ScrollingUtil.canChildScrollUp(targetView) && scrollTempOffset < 0 && moveDistance >= 0) {
                onOverScrollUp();
            } else if (!isOverScrollTrigger && !ScrollingUtil.canChildScrollDown(targetView) && scrollTempOffset > 0 && moveDistance <= 0) {
                onOverScrollDown();
            }
            overScrollLogic(scrollTempOffset);
        }
    }

    private int getFinalOverScrollDistance() {
        return (int) (Math.pow(Math.abs(getScrollerAbleDistance()) * twinkDuringAdjustValue, 0.4));
    }

    private int getScrollerAbleDistance() {
        return scroller.getFinalY() - scroller.getCurrY();
    }

    /**
     * scroll over logic
     *
     * @param tempDistance scroll distance
     */
    private void overScrollLogic(int tempDistance) {
        if (overScrollState == 1 || overScrollState == 2) {
            isOverScrollTrigger = true;
            startOverScrollAnimation(tempDistance < 0
                    ? Math.max(-overScrollMaxTriggerOffset, tempDistance)
                    : Math.min(overScrollMaxTriggerOffset, tempDistance));
        }
    }

    /**
     * dell over scroll to move children
     */
    private void startOverScrollAnimation(final int distanceMove) {
        final int finalDistance = getFinalOverScrollDistance();
        overScrollState = 0;
        if (cancelAllAnimation(scrollAnimation)) {
            return;
        }
        abortScroller();
        if (scrollAnimation == null) {
            scrollAnimation = ValueAnimator.ofInt(distanceMove, 0);
            scrollAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    onNestedScroll(pullContentView, 0, 0, 0, (int) ((Integer) animation.getAnimatedValue() * overScrollDampingRatio));
                }
            });
            scrollAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    onNestedScrollAccepted(pullContentView, pullContentView, 2);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    onStopNestedScroll(pullContentView);
                }
            });
//            scrollAnimation.setInterpolator(new DecelerateInterpolator(1f));
        } else {
            scrollAnimation.setIntValues(distanceMove, 0);
        }
        scrollAnimation.setDuration(getAnimationTime(finalDistance));
        scrollAnimation.start();
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
        refreshShowHelper.layout(0, 0, getMeasuredWidth(), getMeasuredHeight());
        pullContentView.layout(0, 0, getMeasuredWidth(), getMeasuredHeight());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        abortScroller();
        cancelAllAnimation();
    }

    /**
     * dell the nestedScroll
     *
     * @param distanceY move distance of Y
     */
    private void onScroll(float distanceY) {
        if (checkMoving(distanceY) || distanceY == 0) {
            return;
        }
        moveDistance += distanceY;
        if (pullLimitDistance != -1) {
            moveDistance = Math.min(moveDistance, pullLimitDistance);
            moveDistance = Math.max(moveDistance, -pullLimitDistance);
        }

        if (!pullTwinkEnable && ((refreshState == 1 && moveDistance < 0)
                || (refreshState == 2 && moveDistance > 0))) {
            moveDistance = 0;
            return;
        }
        if ((pullLoadMoreEnable && moveDistance <= 0)
                || (pullRefreshEnable && moveDistance >= 0) || pullTwinkEnable) {
            moveChildren(moveDistance);
        } else {
            moveDistance = 0;
            return;
        }

        if (moveDistance >= 0) {
            if (headerView != null && headerView instanceof OnPullListener) {
                ((OnPullListener) headerView).onPullChange((float) moveDistance / refreshTriggerDistance);
            }
            if (moveDistance >= refreshTriggerDistance) {
                if (pullStateControl) {
                    pullStateControl = false;
                    if (headerView != null && refreshState == 0 && headerView instanceof OnPullListener) {
                        ((OnPullListener) headerView).onPullHoldTrigger();
                    }
                }
                return;
            }
            if (!pullStateControl) {
                pullStateControl = true;
                if (headerView != null && refreshState == 0 && headerView instanceof OnPullListener) {
                    ((OnPullListener) headerView).onPullHoldUnTrigger();
                }
            }
            return;
        }
        if (footerView != null && footerView instanceof OnPullListener) {
            ((OnPullListener) footerView).onPullChange((float) moveDistance / loadTriggerDistance);
        }
        if (moveDistance <= -loadTriggerDistance) {
            if (pullStateControl) {
                pullStateControl = false;
                if (footerView != null && refreshState == 0 && footerView instanceof OnPullListener) {
                    ((OnPullListener) footerView).onPullHoldTrigger();
                }
            }
            return;
        }
        if (!pullStateControl) {
            pullStateControl = true;
            if (footerView != null && refreshState == 0 && footerView instanceof OnPullListener) {
                ((OnPullListener) footerView).onPullHoldUnTrigger();
            }
        }
    }

    /**
     * check before header down and footer up moving
     *
     * @param distanceY just make sure the move direct
     * @return need intercept
     */
    private boolean checkMoving(float distanceY) {
        return (((distanceY > 0 && moveDistance == 0) || moveDistance > 0) && onDragIntercept != null
                && !onDragIntercept.onHeaderDownIntercept()) ||
                (((distanceY < 0 && moveDistance == 0) || moveDistance < 0) && onDragIntercept != null
                        && !onDragIntercept.onFooterUpIntercept());
    }

    /**
     * move children
     */
    private void moveChildren(int distance) {
        moveDistance = distance;
        dellAutoLoading();
        refreshShowHelper.dellHeaderFooterMoving(moveDistance);
        ViewCompat.setTranslationY(pullContentView, moveDistance);
    }

    private void dellAutoLoading() {
        if (moveDistance <= 0 && autoLoadingEnable && refreshState == 0
                && onRefreshListener != null && !ScrollingUtil.canChildScrollDown(targetView) && !isAutoLoadTrigger) {
            isAutoLoadTrigger = true;
            onRefreshListener.onLoading();
        }
    }

    /**
     * decide on the action refresh or loadMore
     */
    private void handleAction() {
        if (pullRefreshEnable && refreshState != 2 && !isResetTrigger && moveDistance >= refreshTriggerDistance) {
            startRefresh(moveDistance, true);
        } else if ((moveDistance > 0 && refreshState != 1) || (isResetTrigger && refreshState == 1)) {
            resetHeaderView(moveDistance);
        }

        if (pullLoadMoreEnable && refreshState != 1 && !isResetTrigger && moveDistance <= -loadTriggerDistance) {
            startLoadMore(moveDistance);
        } else if ((moveDistance < 0 && refreshState != 2) || (isResetTrigger && refreshState == 2)) {
            resetFootView(moveDistance);
        }
    }

    private void startRefresh(int headerViewHeight, final boolean withAction) {
        if (headerView != null && !isHoldingTrigger && headerView instanceof OnPullListener) {
            ((OnPullListener) headerView).onPullHolding();
            isHoldingTrigger = true;
        }
        if (!cancelAllAnimation(startRefreshAnimator)) {
            if (startRefreshAnimator == null) {
                startRefreshAnimator = ValueAnimator.ofInt(headerViewHeight, refreshTriggerDistance);
                startRefreshAnimator.addUpdateListener(headerAnimationUpdate);
                startRefreshAnimator.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        if (refreshState == 0 || isAutoRefreshTrigger) {
                            isAutoRefreshTrigger = false;
                            refreshState = 1;
                            if (footerView != null) {
                                footerView.setVisibility(GONE);
                            }
                            if (onRefreshListener != null && refreshWithAction) {
                                onRefreshListener.onRefresh();
                            }
                        }
                    }
                });
                if (headerViewHeight == 0) {
                    startRefreshAnimator.setDuration(refreshBackTime);
                } else {
                    startRefreshAnimator.setDuration(getAnimationTime(moveDistance));
                }
            } else {
                startRefreshAnimator.setIntValues(headerViewHeight, refreshTriggerDistance);
            }
            refreshWithAction = withAction;
            startRefreshAnimator.setInterpolator(new DecelerateInterpolator(2f));
            startRefreshAnimator.start();
        }
    }

    private void resetHeaderView(int headerViewHeight) {
        if (headerViewHeight <= 0 && refreshState == 1) {
            resetRefreshState();
            return;
        }
        if (!cancelAllAnimation(resetHeaderAnimator)) {
            if (resetHeaderAnimator == null) {
                resetHeaderAnimator = ValueAnimator.ofInt(headerViewHeight, 0);
                resetHeaderAnimator.addUpdateListener(headerAnimationUpdate);
                resetHeaderAnimator.addListener(resetHeaderAnimation);
            } else {
                resetHeaderAnimator.setIntValues(headerViewHeight, 0);
            }
            resetHeaderAnimator.setDuration(refreshBackTime != -1 ? refreshBackTime : getAnimationTime(moveDistance));
            resetHeaderAnimator.start();
        }
    }

    private void resetRefreshState() {
        if (headerView != null && headerView instanceof OnPullListener) {
            ((OnPullListener) headerView).onPullReset();
        }
        if (footerView != null) {
            footerView.setVisibility(VISIBLE);
        }
        resetState();
    }

    private void startLoadMore(int loadMoreViewHeight) {
        if (footerView != null && !isHoldingTrigger && footerView instanceof OnPullListener) {
            ((OnPullListener) footerView).onPullHolding();
            isHoldingTrigger = true;
        }
        if (!cancelAllAnimation(startLoadMoreAnimator)) {
            if (startLoadMoreAnimator == null) {
                startLoadMoreAnimator = ValueAnimator.ofInt(loadMoreViewHeight, -loadTriggerDistance);
                startLoadMoreAnimator.addUpdateListener(footerAnimationUpdate);
                startLoadMoreAnimator.addListener(loadingStartAnimation);
                startLoadMoreAnimator.setInterpolator(new DecelerateInterpolator(2f));
            } else {
                startLoadMoreAnimator.setIntValues(loadMoreViewHeight, -loadTriggerDistance);
            }
            startLoadMoreAnimator.setDuration(getAnimationTime(moveDistance));
            startLoadMoreAnimator.start();
        }
    }

    private void resetFootView(int loadMoreViewHeight) {
        if (loadMoreViewHeight >= 0 && refreshState == 2) {
            resetLoadMoreState();
            return;
        }
        if (!cancelAllAnimation(resetFootAnimator)) {
            if (resetFootAnimator == null) {
                resetFootAnimator = ValueAnimator.ofInt(loadMoreViewHeight, 0);
                resetFootAnimator.addUpdateListener(footerAnimationUpdate);
                resetFootAnimator.addListener(resetFooterAnimation);
            } else {
                resetFootAnimator.setIntValues(loadMoreViewHeight, 0);
            }
            resetFootAnimator.setDuration(refreshBackTime != -1 ? refreshBackTime : getAnimationTime(moveDistance));
            resetFootAnimator.start();
        }
    }

    private void resetLoadMoreState() {
        if (footerView != null && footerView instanceof OnPullListener) {
            ((OnPullListener) footerView).onPullReset();
        }
        if (headerView != null) {
            headerView.setVisibility(VISIBLE);
        }
        resetState();
    }

    public void refreshComplete() {
        if (refreshState == 1) {
            isResetTrigger = true;
            resetHeaderView(moveDistance);
        }
    }

    public void loadMoreComplete() {
        if (refreshState == 2) {
            isResetTrigger = true;
            resetFootView(moveDistance);
        }
        isAutoLoadTrigger = false;
    }

    public void autoRefresh() {
        autoRefresh(true);
    }

    public void autoRefresh(boolean withAction) {
        if (pullContentView == null || !pullRefreshEnable) {
            return;
        }

        isAutoRefreshTrigger = true;
        refreshState = 1;
        startRefresh(moveDistance, withAction);
    }

    private void resetState() {
        isResetTrigger = false;
        isHoldingTrigger = false;
        isHoldingFinishTrigger = false;
        pullStateControl = true;
        refreshState = 0;
    }

    private void abortScroller() {
        if (scroller != null && !scroller.isFinished()) {
            scroller.abortAnimation();
        }
    }

    private void cancelAnimation(ValueAnimator animator) {
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
    }

    private boolean cancelAllAnimation(ValueAnimator animator) {
        if (animator != null && animator.isRunning()) {
            return true;
        }
        cancelAnimation(scrollAnimation);
        cancelAnimation(startRefreshAnimator);
        cancelAnimation(resetHeaderAnimator);
        cancelAnimation(startLoadMoreAnimator);
        cancelAnimation(resetFootAnimator);
        return false;
    }

    private boolean cancelAllAnimation() {
        return cancelAllAnimation(null);
    }

    private long getAnimationTime(int moveDistance) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        float ratio = Math.abs((float) moveDistance / (float) displayMetrics.heightPixels);
        return (long) (Math.pow(2000 * ratio, 0.5) * duringAdjustValue);
    }

    private int dipToPx(Context context, float value) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, metrics);
    }


    /**
     * state animation
     */
    private final AnimatorListenerAdapter resetHeaderAnimation = new AnimatorListenerAdapter() {
        private boolean isCancel;

        @Override
        public void onAnimationStart(Animator animation) {
            if (headerView != null && refreshState == 1 && !isHoldingFinishTrigger && headerView instanceof OnPullListener) {
                ((OnPullListener) headerView).onPullFinish();
                isHoldingFinishTrigger = true;
            }
        }

        public void onAnimationCancel(Animator animation) {
            isCancel = true;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (refreshState == 1 && !isCancel) {
                resetRefreshState();
            }
            isCancel = false;
        }
    };

    private final AnimatorListenerAdapter resetFooterAnimation = new AnimatorListenerAdapter() {
        private boolean isCancel;

        @Override
        public void onAnimationStart(Animator animation) {
            if (footerView != null && refreshState == 2 && !isHoldingFinishTrigger && footerView instanceof OnPullListener) {
                ((OnPullListener) footerView).onPullFinish();
                isHoldingFinishTrigger = true;
            }
        }

        public void onAnimationCancel(Animator animation) {
            isCancel = true;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (refreshState == 2 && !isCancel) {
                resetLoadMoreState();
            }
            isCancel = false;
        }
    };

    private final AnimatorListenerAdapter loadingStartAnimation = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            if (onRefreshListener != null && refreshState == 0) {
                if (headerView != null) {
                    headerView.setVisibility(GONE);
                }
                refreshState = 2;
                onRefreshListener.onLoading();
            }
        }
    };

    /**
     * animator update listener
     */
    private final ValueAnimator.AnimatorUpdateListener headerAnimationUpdate = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            moveChildren((Integer) animation.getAnimatedValue());
            if (headerView != null && headerView instanceof OnPullListener) {
                ((OnPullListener) headerView).onPullChange((float) moveDistance / refreshTriggerDistance);
            }
        }
    };

    private final ValueAnimator.AnimatorUpdateListener footerAnimationUpdate = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            moveChildren((Integer) animation.getAnimatedValue());
            if (footerView != null && footerView instanceof OnPullListener) {
                ((OnPullListener) footerView).onPullChange((float) moveDistance / loadTriggerDistance);
            }
        }
    };

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        handleAction();
        cancelAllAnimation();
        abortScroller();
        overScrollState = 0;
        finalScrollDistance = -1;
        isOverScrollTrigger = false;
        return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        parentHelper.onNestedScrollAccepted(child, target, axes);
        startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
    }

    @Override
    public void onStopNestedScroll(View child) {
        if (!pullTwinkEnable || scroller != null && scroller.isFinished()) {
            handleAction();
        }
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
        } else if (dy < 0 && moveDistance < 0) {
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
        return  dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        if ((pullTwinkEnable || autoLoadingEnable)) {
            readyScroller();
            abortScroller();
            scroller.fling(0, 0, 0, (int) velocityY, 0, 0, -Integer.MAX_VALUE, Integer.MAX_VALUE);
            finalScrollDistance = getScrollerAbleDistance();
            lastScrollY = 0;
            invalidate();
        }
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
        return !generalPullHelper.onInterceptTouchEvent(ev) && super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        return generalPullHelper.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        generalPullHelper.dellDirection(ev);

        if (pullContentView instanceof NestedScrollingChild) {
            if (ev.getActionMasked() == MotionEvent.ACTION_UP
                    || ev.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                onStopNestedScroll(pullContentView);
            }
            return super.dispatchTouchEvent(ev);
        }

        return !generalPullHelper.dispatchTouchEvent(ev, finalMotionEvent)
                && super.dispatchTouchEvent(finalMotionEvent[0]);
    }

    public void setHeaderView(View header) {
        if (header == null) {
            return;
        }
        if (headerView != null && headerView != header) {
            removeView(headerView);
        }
        LayoutParams lp = header.getLayoutParams();
        if (lp == null) {
            lp = new LayoutParams(-1, -2);
            header.setLayoutParams(lp);
        }
        headerView = header;
        addView(header);

        if (targetView != null) {
            targetView.bringToFront();
        }
    }

    public void setFooterView(View footer) {
        if (footer == null) {
            return;
        }
        if (footerView != null && footerView != footer) {
            removeView(footerView);
        }
        LayoutParams lp = footer.getLayoutParams();
        if (lp == null) {
            lp = new LayoutParams(-1, -2);
            footer.setLayoutParams(lp);
        }
        footerView = footer;
        addView(footer);

        if (targetView != null) {
            targetView.bringToFront();
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

    public void setTwinkDuringAdjustValue(float twinkDuringAdjustValue) {
        this.twinkDuringAdjustValue = twinkDuringAdjustValue;
    }

    private void setScrollInterpolator(Interpolator interpolator) {
        scroller = ScrollerCompat.create(getContext(), interpolator);
    }

    public void setRefreshTriggerDistance(int refreshTriggerDistance) {
        isHeaderHeightSet = true;
        this.refreshTriggerDistance = refreshTriggerDistance;
    }

    public void setLoadTriggerDistance(int loadTriggerDistance) {
        isFooterHeightSet = true;
        this.loadTriggerDistance = loadTriggerDistance;
    }

    public void setPullLimitDistance(int pullLimitDistance) {
        this.pullLimitDistance = pullLimitDistance;
    }

    public void setOverScrollMaxTriggerOffset(int overScrollMaxTriggerOffset) {
        this.overScrollMaxTriggerOffset = overScrollMaxTriggerOffset;
    }

    public void setDragDampingRatio(float dragDampingRatio) {
        this.dragDampingRatio = dragDampingRatio;
    }

    public void setDuringAdjustValue(float duringAdjustValue) {
        this.duringAdjustValue = duringAdjustValue;
    }

    public void setRefreshBackTime(int refreshBackTime) {
        this.refreshBackTime = refreshBackTime;
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
        return refreshState != 0;
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
}