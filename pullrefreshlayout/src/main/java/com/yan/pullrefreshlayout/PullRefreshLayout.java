package com.yan.pullrefreshlayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ListViewCompat;
import android.support.v4.widget.ScrollerCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ListView;
import android.widget.ScrollView;

/**
 * Created by yan on 2017/4/11.
 */
public class PullRefreshLayout extends ViewGroup implements NestedScrollingParent, NestedScrollingChild {
    private final NestedScrollingParentHelper parentHelper;
    private final NestedScrollingChildHelper childHelper;
    private final int[] parentScrollConsumed = new int[2];
    private final int[] parentOffsetInWindow = new int[2];

    /**
     * view children
     */
    View headerView;
    View footerView;
    View targetView;

    View pullContentView;

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
     * switch
     */
    private boolean pullRefreshEnable = true;
    private boolean pullTwinkEnable = true;
    private boolean pullLoadMoreEnable = false;
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

    private boolean isListScrollBackScroll;
    private boolean isScrollerViewBackScroll;

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
    private Interpolator scrollInterpolator;

    private ValueAnimator startRefreshAnimator;
    private ValueAnimator resetHeaderAnimator;
    private ValueAnimator startLoadMoreAnimator;
    private ValueAnimator resetFootAnimator;
    private ValueAnimator overScrollAnimation;

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

        refreshTriggerDistance = PullRefreshLayoutUtil.dipToPx(context, refreshTriggerDistance);
        loadTriggerDistance = PullRefreshLayoutUtil.dipToPx(context, loadTriggerDistance);
        if (typedArray.hasValue(R.styleable.PullRefreshLayout_prl_refreshTriggerDistance)) {
            isHeaderHeightSet = true;
            refreshTriggerDistance = typedArray.getDimensionPixelOffset(R.styleable.PullRefreshLayout_prl_refreshTriggerDistance, refreshTriggerDistance);
        }
        if (typedArray.hasValue(R.styleable.PullRefreshLayout_prl_loadTriggerDistance)) {
            isFooterHeightSet = true;
            loadTriggerDistance = typedArray.getDimensionPixelOffset(R.styleable.PullRefreshLayout_prl_loadTriggerDistance, loadTriggerDistance);
        }
        pullLimitDistance = typedArray.getDimensionPixelOffset(R.styleable.PullRefreshLayout_prl_pullLimitDistance, pullLimitDistance);
        overScrollMaxTriggerOffset = typedArray.getDimensionPixelOffset(R.styleable.PullRefreshLayout_prl_overScrollMaxTriggerOffset, PullRefreshLayoutUtil.dipToPx(context, overScrollMaxTriggerOffset));

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

    private void initHeaderOrFooter(Context context, TypedArray typedArray) {
        headerView = PullRefreshLayoutUtil.parseClassName(context, typedArray.getString(R.styleable.PullRefreshLayout_prl_headerClass));
        if (headerView == null) {
            int headerViewId = typedArray.getResourceId(R.styleable.PullRefreshLayout_prl_headerViewId, View.NO_ID);
            if (headerViewId != View.NO_ID) {
                headerView = LayoutInflater.from(context).inflate(headerViewId, null, false);
            }
        }
        footerView = PullRefreshLayoutUtil.parseClassName(context, typedArray.getString(R.styleable.PullRefreshLayout_prl_footerClass));
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
        getPullContentView();
        if (targetView == null) {
            targetView = pullContentView;
        }
        if ((pullTwinkEnable || autoLoadingEnable)) {
            readyScroller();
        }
    }

    /**
     * dell the over scroll block
     *
     * @return
     */
    private RecyclerView.OnScrollListener getRecyclerScrollListener() {
        return new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (PullRefreshLayoutUtil.isRecyclerViewToTop(recyclerView)
                        && generalPullHelper.isMovingDirectDown && !generalPullHelper.isTouch) {
                    overScroll(1, scrollTempOffset);
                } else if (PullRefreshLayoutUtil.isRecyclerViewToBottom(recyclerView)
                        && !generalPullHelper.isMovingDirectDown && !generalPullHelper.isTouch) {
                    overScroll(2, scrollTempOffset);
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
            } else if (isListScrollBackScroll && (pullContentView instanceof ListView)) {
                ListViewCompat.scrollListBy((ListView) pullContentView, scrollTempOffset);
            }

            invalidate();

            if (targetView instanceof RecyclerView) {
                return;
            }

            if (!isOverScrollTrigger && !PullRefreshLayoutUtil.canChildScrollUp(targetView) && scrollTempOffset < 0 && moveDistance >= 0) {
                overScroll(1, scrollTempOffset);
            } else if (!isOverScrollTrigger && !PullRefreshLayoutUtil.canChildScrollDown(targetView) && scrollTempOffset > 0 && moveDistance <= 0) {
                overScroll(2, scrollTempOffset);
            }
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
                scroller = ScrollerCompat.create(getContext(), scrollInterpolator = getRecyclerDefaultInterpolator());
                addRecyclerScrollListener();
            }
            scroller = ScrollerCompat.create(getContext());
        }
    }

    private void addRecyclerScrollListener() {
        ((RecyclerView) targetView).addOnScrollListener(getRecyclerScrollListener());
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
     * overScroll Back Dell
     *
     * @param type         just to make sure the condition
     * @param tempDistance temp move distance
     * @return need continue
     */
    private boolean overScrollBackDell(int type, int tempDistance) {
        if ((type == 1 && moveDistance - tempDistance < 0)
                || (type == 2 && moveDistance - tempDistance > 0)) {
            onScroll(-moveDistance);

            if (pullContentView instanceof ListView) {
                isListScrollBackScroll = true;
                return false;
            } else if (pullContentView instanceof ScrollView && !isScrollerViewBackScroll) {
                isScrollerViewBackScroll = true;
                ((ScrollView) pullContentView).fling((type == 1 ? 1 : -1) * (int) scroller.getCurrVelocity());
                return true;
            } else if (!(pullContentView instanceof NestedScrollingChild)) {
                overScroll(type, tempDistance);
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
     * dell over scroll to move children
     */
    private void startOverScrollAnimation(final int distanceMove) {
        final int finalDistance = getFinalOverScrollDistance();
        cancelAllAnimation();
        abortScroller();
        if (overScrollAnimation == null) {
            overScrollAnimation = ValueAnimator.ofInt(distanceMove, 0);
            overScrollAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    onNestedScroll(pullContentView, 0, 0, 0, (int) ((Integer) animation.getAnimatedValue() * overScrollDampingRatio));
                }
            });
            overScrollAnimation.addListener(new AnimatorListenerAdapter() {
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
//            overScrollAnimation.setInterpolator(new DecelerateInterpolator(1f));
        } else {
            overScrollAnimation.setIntValues(distanceMove, 0);
        }
        overScrollAnimation.setDuration(getAnimationTime(finalDistance));
        overScrollAnimation.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        for (int i = 0; i < getChildCount(); i++) {
            measureChildWithMargins(getChildAt(i), widthMeasureSpec, 0, heightMeasureSpec, 0);
        }
        if (headerView != null && !isHeaderHeightSet) {
            refreshTriggerDistance = headerView.getMeasuredHeight();
        }
        if (footerView != null && !isFooterHeightSet) {
            loadTriggerDistance = footerView.getMeasuredHeight();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        refreshShowHelper.layout(0, 0, getMeasuredWidth(), getMeasuredHeight());
        layoutContentView();
    }

    private void layoutContentView() {
        MarginLayoutParams lp = (MarginLayoutParams) pullContentView.getLayoutParams();
        pullContentView.layout(getPaddingLeft() + lp.leftMargin
                , getPaddingTop() + lp.topMargin
                , getPaddingLeft() + lp.leftMargin + pullContentView.getMeasuredWidth()
                , getPaddingTop() + lp.topMargin + pullContentView.getMeasuredHeight());
    }

    @Override
    public MarginLayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected MarginLayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected boolean checkLayoutParams(LayoutParams p) {
        return p instanceof MarginLayoutParams;
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams lp) {
        return new MarginLayoutParams(lp);
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

    private int getFinalOverScrollDistance() {
        return (int) (Math.pow(Math.abs(getScrollerAbleDistance()) * twinkDuringAdjustValue, 0.4));
    }

    private int getScrollerAbleDistance() {
        return scroller.getFinalY() - scroller.getCurrY();
    }

    private void overScroll(int type, int offset) {
        if (type == 1) {
            onOverScrollUp();
        } else {
            onOverScrollDown();
        }
        isOverScrollTrigger = true;
        int finalScrollOffset = offset < 0 ? Math.max(-overScrollMaxTriggerOffset, offset) : Math.min(overScrollMaxTriggerOffset, offset);
        startOverScrollAnimation(finalScrollOffset);
    }

    private void dellAutoLoading() {
        if (moveDistance <= 0 && autoLoadingEnable && refreshState == 0
                && onRefreshListener != null && !PullRefreshLayoutUtil.canChildScrollDown(targetView) && !isAutoLoadTrigger) {
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
                startRefreshAnimator.addListener(refreshStartAnimation);
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
            resetHeaderAnimation.onAnimationStart(null);
            resetHeaderAnimation.onAnimationEnd(null);
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
            resetFooterAnimation.onAnimationStart(null);
            resetFooterAnimation.onAnimationEnd(null);
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
        if (refreshState != 0 || pullContentView == null || !pullRefreshEnable) {
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
        cancelAnimation(overScrollAnimation);
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
        float ratio = Math.abs((float) moveDistance / PullRefreshLayoutUtil.getWindowHeight(getContext()));
        return (long) (Math.pow(2000 * ratio, 0.5) * duringAdjustValue);
    }

    /**
     * state animation
     */
    private final AnimatorListenerAdapter resetHeaderAnimation = new PullAnimatorListenerAdapter() {
        protected void animationStart() {
            if (headerView != null && refreshState == 1 && !isHoldingFinishTrigger && headerView instanceof OnPullListener) {
                ((OnPullListener) headerView).onPullFinish();
                isHoldingFinishTrigger = true;
            }
        }

        protected void animationEnd() {
            if (refreshState == 1) {
                resetRefreshState();
            }
        }
    };

    private final AnimatorListenerAdapter resetFooterAnimation = new PullAnimatorListenerAdapter() {
        protected void animationStart() {
            if (footerView != null && refreshState == 2 && !isHoldingFinishTrigger && footerView instanceof OnPullListener) {
                ((OnPullListener) footerView).onPullFinish();
                isHoldingFinishTrigger = true;
            }
        }

        protected void animationEnd() {
            if (refreshState == 2) {
                resetLoadMoreState();
            }
        }
    };

    private final AnimatorListenerAdapter refreshStartAnimation = new PullAnimatorListenerAdapter() {
        protected void animationEnd() {
            if ((refreshState == 0 || isAutoRefreshTrigger)) {
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
    };

    private final AnimatorListenerAdapter loadingStartAnimation = new PullAnimatorListenerAdapter() {
        protected void animationEnd() {
            if ((onRefreshListener != null && refreshState == 0)) {
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
        abortScroller();
        cancelAllAnimation();
        overScrollState = 0;
        finalScrollDistance = -1;
        isOverScrollTrigger = false;
        isListScrollBackScroll = false;
        isScrollerViewBackScroll = false;
        return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        parentHelper.onNestedScrollAccepted(child, target, axes);
        startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
    }

    @Override
    public void onStopNestedScroll(View child) {
        if (!generalPullHelper.isTouch && (!pullTwinkEnable || scroller != null && scroller.isFinished())) {
            handleAction();
        }
        parentHelper.onStopNestedScroll(child);
        stopNestedScroll();
    }

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
        return dispatchNestedPreFling(velocityX, velocityY);
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
    public boolean dispatchTouchEvent(MotionEvent ev) {
        generalPullHelper.dispatchTouchEvent(ev, finalMotionEvent);
        super.dispatchTouchEvent(finalMotionEvent[0]);
        return true;
    }

    public void setHeaderView(View header) {
        setRefreshView(1, header);
    }

    public void setFooterView(View footer) {
        setRefreshView(2, footer);
    }

    private void setRefreshView(int type, View inView) {
        if (inView == null) {
            return;
        }
        if (type == 1) {
            if (headerView != null && headerView != inView) {
                removeView(headerView);
            }
            headerView = inView;
        } else if (type == 2) {
            if (footerView != null && footerView != inView) {
                removeView(footerView);
            }
            footerView = inView;
        }
        LayoutParams lp = inView.getLayoutParams();
        if (lp == null) {
            lp = new LayoutParams(-1, -2);
            inView.setLayoutParams(lp);
        }
        addView(inView);

        if (targetView != null) {
            targetView.bringToFront();
        }
    }

    public void setTargetView(View targetView) {
        this.targetView = targetView;
        if ((targetView instanceof RecyclerView) && (pullTwinkEnable || autoLoadingEnable)) {
            if (scrollInterpolator == null) {
                scroller = ScrollerCompat.create(getContext(), scrollInterpolator = getRecyclerDefaultInterpolator());
            }
            addRecyclerScrollListener();
        }
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
        this.scrollInterpolator = interpolator;
        scroller = ScrollerCompat.create(getContext(), scrollInterpolator);
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

    public int getOverScrollState() {
        return overScrollState;
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

    private abstract class PullAnimatorListenerAdapter extends AnimatorListenerAdapter {
        private boolean isCancel;

        public void onAnimationStart(Animator animation) {
            animationStart();
        }

        public void onAnimationCancel(Animator animation) {
            isCancel = true;
        }

        public void onAnimationEnd(Animator animation) {
            if (!isCancel) {
                animationEnd();
            }
            isCancel = false;
        }

        protected void animationStart() {
        }

        protected void animationEnd() {
        }
    }
}