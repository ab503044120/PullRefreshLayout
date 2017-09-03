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
import android.webkit.WebView;
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
     * - use by showGravity to dell onLayout
     */
    View headerView;
    View footerView;

    private View targetView;
    private View pullContentLayout;

    //-------------------------START| values part |START-----------------------------

    /**
     * trigger distance
     * - use by showGravity to control the layout move
     */
    int refreshTriggerDistance = 60;
    int loadTriggerDistance = 60;

    /**
     * max drag distance
     */
    private int pullDownLimitDistance = 0;
    private int pullUpLimitDistance = 0;

    /**
     * animation without overScroll total during
     */
    private int animationDuring = 300;

    /**
     * over scroll start offset
     */
    private int overScrollMaxTriggerOffset = 50;

    /**
     * targetViewId
     */
    private int targetViewId = -1;

    /**
     * the ratio for final distance for drag
     */
    private float dragDampingRatio = 0.6F;

    /**
     * overScrollAdjustValue
     */
    private float overScrollAdjustValue = 1F;

    /**
     * move distance ratio for over scroll
     */
    private float overScrollDampingRatio = 0.2F;

    /**
     * switch
     */
    private boolean pullRefreshEnable = true;
    private boolean pullTwinkEnable = true;
    private boolean pullLoadMoreEnable = false;
    private boolean autoLoadingEnable = false;

    /**
     * dispatch Pull Touch Able
     */
    private boolean dispatchPullTouchAble = true;

    /**
     * move With
     * isMoveWithContent:- use by generalHelper dell touch logic
     */
    private boolean isMoveWithFooter = true;
    private boolean isMoveWithHeader = true;
    boolean isMoveWithContent = true;

    /**
     * view front
     */
    private boolean isHeaderFront = false;
    private boolean isFooterFront = false;

    //--------------------START|| values can modify in the lib only ||START------------------

    /**
     * current refreshing state 1:refresh 2:loadMore
     */
    private int refreshState = 0;

    /**
     * last Scroll Y
     */
    private int lastScrollY = 0;
    private int currScrollOffset = 0;

    /**
     * over scroll state
     */
    private int overScrollState = 0;

    /**
     * drag move distance
     * - use by generalHelper dell touch logic
     */
    int moveDistance = 0;

    /**
     * final scroll distance
     */
    private float finalScrollDistance = -1;

    /**
     * make sure header or footer hold trigger one time
     */
    private boolean pullStateControl = true;

    /**
     * refreshing state trigger
     */
    private boolean isHoldingTrigger = false;
    private boolean isHoldingFinishTrigger = false;
    private boolean isResetTrigger = false;
    private boolean isOverScrollTrigger = false;
    private boolean isAutoLoadingTrigger = false;

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
     * isScrollAbleViewBackScroll
     */
    private boolean isScrollAbleViewBackScroll = false;

    /**
     * is isTargetNested
     */
    private boolean isTargetNested = false;

    private boolean isAttachWindow = false;

    //--------------------END|| values can modify int class only ||END------------------
    //--------------------END| values part |END------------------

    private final ShowGravity showGravity;
    private final GeneralPullHelper generalPullHelper;

    private OnRefreshListener onRefreshListener;
    private OnDragIntercept onDragIntercept;

    private ScrollerCompat scroller;
    private Interpolator scrollInterpolator;

    private RecyclerView.OnScrollListener defaultScrollListener;

    private ValueAnimator startRefreshAnimator;
    private ValueAnimator resetHeaderAnimator;
    private ValueAnimator startLoadMoreAnimator;
    private ValueAnimator resetFooterAnimator;
    private ValueAnimator overScrollAnimator;

    private Runnable delayHandleActionRunnable;

    public PullRefreshLayout(Context context) {
        this(context, null, 0);
    }

    public PullRefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        showGravity = new ShowGravity(this);
        generalPullHelper = new GeneralPullHelper(this, context);

        parentHelper = new NestedScrollingParentHelper(this);
        childHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);

        loadAttribute(context, attrs);
    }

    private void loadAttribute(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.PullRefreshLayout);
        pullRefreshEnable = ta.getBoolean(R.styleable.PullRefreshLayout_prl_refreshEnable, pullRefreshEnable);
        pullLoadMoreEnable = ta.getBoolean(R.styleable.PullRefreshLayout_prl_loadMoreEnable, pullLoadMoreEnable);
        pullTwinkEnable = ta.getBoolean(R.styleable.PullRefreshLayout_prl_twinkEnable, pullTwinkEnable);
        autoLoadingEnable = ta.getBoolean(R.styleable.PullRefreshLayout_prl_autoLoadingEnable, autoLoadingEnable);
        isHeaderFront = ta.getBoolean(R.styleable.PullRefreshLayout_prl_headerFront, isHeaderFront);
        isFooterFront = ta.getBoolean(R.styleable.PullRefreshLayout_prl_footerFront, isFooterFront);

        isHeaderHeightSet = ta.hasValue(R.styleable.PullRefreshLayout_prl_refreshTriggerDistance);
        isFooterHeightSet = ta.hasValue(R.styleable.PullRefreshLayout_prl_loadTriggerDistance);
        refreshTriggerDistance = ta.getDimensionPixelOffset(R.styleable.PullRefreshLayout_prl_refreshTriggerDistance, CommonUtils.dipToPx(context, refreshTriggerDistance));
        loadTriggerDistance = ta.getDimensionPixelOffset(R.styleable.PullRefreshLayout_prl_loadTriggerDistance, CommonUtils.dipToPx(context, loadTriggerDistance));

        pullDownLimitDistance = ta.getDimensionPixelOffset(R.styleable.PullRefreshLayout_prl_pullDownLimitDistance, 0);
        pullUpLimitDistance = ta.getDimensionPixelOffset(R.styleable.PullRefreshLayout_prl_pullUpLimitDistance, 0);

        animationDuring = ta.getInt(R.styleable.PullRefreshLayout_prl_animationDuring, animationDuring);
        dragDampingRatio = ta.getFloat(R.styleable.PullRefreshLayout_prl_dragDampingRatio, dragDampingRatio);

        overScrollAdjustValue = ta.getFloat(R.styleable.PullRefreshLayout_prl_overScrollAdjustValue, overScrollAdjustValue);
        overScrollDampingRatio = ta.getFloat(R.styleable.PullRefreshLayout_prl_overScrollDampingRatio, overScrollDampingRatio);
        overScrollMaxTriggerOffset = ta.getDimensionPixelOffset(R.styleable.PullRefreshLayout_prl_overScrollMaxTriggerOffset, CommonUtils.dipToPx(context, overScrollMaxTriggerOffset));

        showGravity.headerShowGravity = ta.getInteger(R.styleable.PullRefreshLayout_prl_headerShowGravity, ShowGravity.FOLLOW);
        showGravity.footerShowGravity = ta.getInteger(R.styleable.PullRefreshLayout_prl_footerShowGravity, ShowGravity.FOLLOW);

        targetViewId = ta.getResourceId(R.styleable.PullRefreshLayout_prl_targetId, targetViewId);

        headerView = initRefreshView(context, ta.getString(R.styleable.PullRefreshLayout_prl_headerClass), ta.getResourceId(R.styleable.PullRefreshLayout_prl_headerViewId, -1));
        footerView = initRefreshView(context, ta.getString(R.styleable.PullRefreshLayout_prl_footerClass), ta.getResourceId(R.styleable.PullRefreshLayout_prl_footerViewId, -1));

        ta.recycle();
    }

    private View initRefreshView(Context context, String className, int viewId) {
        View v = CommonUtils.parseClassName(context, className);
        if (v == null) {
            if (viewId != -1) {
                v = LayoutInflater.from(context).inflate(viewId, null, false);
            }
        }
        return v;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initContentView();
        dellNestedScrollCheck(); // make sure that targetView able to scroll after targetView has set
        readyScroller();
    }

    private void initContentView() {
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i) != footerView && getChildAt(i) != headerView) {
                pullContentLayout = getChildAt(i);
                /**
                 * targetView ready
                 */
                if (targetViewId != -1) {
                    targetView = findViewById(targetViewId);
                }
                if (targetView == null) {
                    targetView = pullContentLayout;
                }

                setHeaderView(headerView);
                setFooterView(footerView);
                return;
            }
        }
        throw new RuntimeException("PullRefreshLayout should have a child");
    }

    public void dispatchSuperTouchEvent(MotionEvent ev) {
        super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (!dispatchPullTouchAble) {
            return super.dispatchTouchEvent(ev);
        }
        generalPullHelper.dispatchTouchEvent(ev);
        dispatchSuperTouchEvent(ev);
        return true;
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

        if (pullDownLimitDistance == 0) {
            pullDownLimitDistance = getMeasuredHeight();
        }
        if (pullUpLimitDistance == 0) {
            pullUpLimitDistance = getMeasuredHeight();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        showGravity.layout(0, 0, getMeasuredWidth(), getMeasuredHeight());
        layoutContentView();
    }

    private void layoutContentView() {
        MarginLayoutParams lp = (MarginLayoutParams) pullContentLayout.getLayoutParams();
        pullContentLayout.layout(getPaddingLeft() + lp.leftMargin
                , getPaddingTop() + lp.topMargin
                , getPaddingLeft() + lp.leftMargin + pullContentLayout.getMeasuredWidth()
                , getPaddingTop() + lp.topMargin + pullContentLayout.getMeasuredHeight());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        isAttachWindow = true;
        handleAction();
    }

    @Override
    protected void onDetachedFromWindow() {
        isAttachWindow = false;

        removeDelayRunnable();
        cancelAllAnimation();
        abortScroller();

        startRefreshAnimator = null;
        resetHeaderAnimator = null;
        startLoadMoreAnimator = null;
        resetFooterAnimator = null;
        overScrollAnimator = null;

        delayHandleActionRunnable = null;

        super.onDetachedFromWindow();
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-1, -1);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    public void computeScroll() {
        boolean isFinish = scroller == null || !scroller.computeScrollOffset() || scroller.isFinished();
        if (!isFinish) {
            int currY = scroller.getCurrY();
            currScrollOffset = currY - lastScrollY;
            lastScrollY = currY;

            if (pullTwinkEnable && ((overScrollFlingState() == 1 && overScrollBackDell(1, currScrollOffset))
                    || (overScrollFlingState() == 2 && overScrollBackDell(2, currScrollOffset)))) {
                return;

                // ListView scroll back scroll to normal
            } else if (isScrollAbleViewBackScroll && (pullContentLayout instanceof ListView)) {
                ListViewCompat.scrollListBy((ListView) pullContentLayout, currScrollOffset);
            }

            // invalidate View ,the method invalidate() sometimes not work , so i use ViewCompat.postInvalidateOnAnimation(this) instead of invalidate()
            ViewCompat.postInvalidateOnAnimation(this);

            if (targetView instanceof RecyclerView) {
                return;
            }

            if (!isOverScrollTrigger && !isTargetAbleScrollUp() && currScrollOffset < 0 && moveDistance >= 0) {
                overScrollDell(1, currScrollOffset);
            } else if (!isOverScrollTrigger && !isTargetAbleScrollDown() && currScrollOffset > 0 && moveDistance <= 0) {
                overScrollDell(2, currScrollOffset);
            }
        }
    }

    /**
     * overScroll Back Dell
     *
     * @param tempDistance temp move distance
     * @return need continue
     */
    private boolean overScrollBackDell(int type, int tempDistance) {
        if ((type == 1 && (finalScrollDistance > moveDistance)) || (type == 2 && finalScrollDistance < moveDistance)) {
            cancelAllAnimation();
            if ((type == 1 && moveDistance <= tempDistance) || (type == 2 && moveDistance >= tempDistance)) {
                dellScroll(-moveDistance);
                return kindsOfViewsToNormalDell(type, tempDistance);
            }
            dellScroll(-tempDistance);
            return false;
        } else {
            abortScroller();
            handleAction();
            return true;
        }
    }

    /**
     * kinds of view dell back scroll to normal state
     */
    private boolean kindsOfViewsToNormalDell(int type, int tempDistance) {
        final int sign = type == 1 ? 1 : -1;
        int velocity = (int) (sign * Math.abs(scroller.getCurrVelocity()));

        if (targetView instanceof ListView && !isScrollAbleViewBackScroll) {
        } else if (targetView instanceof ScrollView && !isScrollAbleViewBackScroll) {
            ((ScrollView) targetView).fling(velocity);
        } else if (targetView instanceof WebView && !isScrollAbleViewBackScroll) {
            ((WebView) targetView).flingScroll(0, velocity);
        } else if (targetView instanceof RecyclerView && !isTargetNested && !isScrollAbleViewBackScroll) {
            ((RecyclerView) targetView).fling(0, velocity);
        } else if (targetView instanceof RecyclerView && ((type == 2 && !CommonUtils.canChildScrollUp(targetView))
                || (type == 1 && !CommonUtils.canChildScrollDown(targetView)))) {
            overScrollDell(type, tempDistance);
            return true;
        }
        isScrollAbleViewBackScroll = true;
        return false;
    }

    private void autoLoadingTrigger() {
        if (!isAutoLoadingTrigger && autoLoadingEnable && refreshState == 0 && onRefreshListener != null) {
            isAutoLoadingTrigger = true;
            onRefreshListener.onLoading();
        }
    }

    /**
     * dell over scroll to move children
     */
    private void startOverScrollAnimation(final int distanceMove) {
        int finalDistance = getFinalOverScrollDistance();
        abortScroller();
        cancelAllAnimation();
        if (overScrollAnimator == null) {
            overScrollAnimator = getAnimator(distanceMove, 0, overScrollAnimatorUpdate, overScrollAnimatorListener);
        } else {
            overScrollAnimator.setIntValues(distanceMove, 0);
        }
        overScrollAnimator.setDuration(getOverScrollTime(finalDistance));
        overScrollAnimator.start();
    }

    private void onOverScrollUp() {
        overScrollState = 1;
    }

    private void onOverScrollDown() {
        overScrollState = 2;
        autoLoadingTrigger();
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
                if (!isTargetAbleScrollUp() && generalPullHelper.isMovingDirectDown && generalPullHelper.dragState == 0) {
                    overScrollDell(1, -Math.abs(currScrollOffset));
                } else if (!isTargetAbleScrollDown() && !generalPullHelper.isMovingDirectDown && generalPullHelper.dragState == 0) {
                    overScrollDell(2, Math.abs(currScrollOffset));
                }
            }
        };
    }

    private void readyScroller() {
        if (scroller == null && flingAble()) {
            if (targetView instanceof RecyclerView) {
                scroller = ScrollerCompat.create(getContext(), scrollInterpolator == null
                        ? scrollInterpolator = getRecyclerDefaultInterpolator() : scrollInterpolator);
                addRecyclerScrollListener();
                return;
            }
            scroller = ScrollerCompat.create(getContext());
        }
    }

    private void addRecyclerScrollListener() {
        RecyclerView targetRecycler = ((RecyclerView) targetView);
        if (defaultScrollListener != null) {
            targetRecycler.removeOnScrollListener(defaultScrollListener);
            targetRecycler.addOnScrollListener(defaultScrollListener);
            return;
        }
        targetRecycler.addOnScrollListener(defaultScrollListener = getRecyclerScrollListener());
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
     * dell the nestedScroll
     *
     * @param distanceY move distance of Y
     */
    private void dellScroll(float distanceY) {
        if (checkMoving(distanceY) || distanceY == 0) {
            return;
        }
        int tempDistance = (int) (moveDistance + distanceY);
        tempDistance = Math.min(tempDistance, pullDownLimitDistance);
        tempDistance = Math.max(tempDistance, -pullUpLimitDistance);

        if (!pullTwinkEnable && ((refreshState == 1 && tempDistance < 0) || (refreshState == 2 && tempDistance > 0))) {
            if (moveDistance == 0) {
                return;
            }
            tempDistance = 0;
        }
        if ((pullLoadMoreEnable && tempDistance <= 0) || (pullRefreshEnable && tempDistance >= 0) || pullTwinkEnable) {
            moveChildren(tempDistance);
        } else {
            moveDistance = 0;
            return;
        }

        if (moveDistance >= 0) {
            onHeaderPullChange((float) moveDistance / refreshTriggerDistance);
            if (moveDistance >= refreshTriggerDistance) {
                if (pullStateControl) {
                    pullStateControl = false;
                    if (refreshState == 0) {
                        onHeaderPullHoldTrigger();
                    }
                }
                return;
            }
            if (!pullStateControl) {
                pullStateControl = true;
                if (refreshState == 0) {
                    onHeaderPullHoldUnTrigger();
                }
            }
            return;
        }
        onFooterPullChange((float) moveDistance / loadTriggerDistance);
        if (moveDistance <= -loadTriggerDistance) {
            if (pullStateControl) {
                pullStateControl = false;
                if (refreshState == 0) {
                    onFooterPullHoldTrigger();
                }
            }
            return;
        }
        if (!pullStateControl) {
            pullStateControl = true;
            if (refreshState == 0) {
                onFooterPullHoldUnTrigger();
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
        return (((distanceY > 0 && moveDistance == 0) || moveDistance > 0) && onDragIntercept != null && !onDragIntercept.onHeaderDownIntercept())
                || (((distanceY < 0 && moveDistance == 0) || moveDistance < 0) && onDragIntercept != null && !onDragIntercept.onFooterUpIntercept());
    }

    private void overScrollDell(int type, int offset) {
        if (pullTwinkEnable // if pullTwinkEnable is true , while fling back the target is able to over scroll just intercept that
                && ((!isTargetAbleScrollUp() && isTargetAbleScrollDown()) && moveDistance < 0
                || (isTargetAbleScrollUp() && !isTargetAbleScrollDown()) && moveDistance > 0)) {
            return;
        }

        if (type == 1) {
            onOverScrollUp();
        } else {
            onOverScrollDown();
        }

        if (!pullTwinkEnable) {
            abortScroller();
            return;
        }

        isOverScrollTrigger = true;

        int finalScrollOffset = offset < 0 ? Math.max(-overScrollMaxTriggerOffset, offset) : Math.min(overScrollMaxTriggerOffset, offset);
        startOverScrollAnimation(finalScrollOffset);
    }

    private void dellAutoLoading() {
        if (moveDistance <= 0 && refreshState == 0 && !isTargetAbleScrollDown()) {
            autoLoadingTrigger();
        }
    }

    /**
     * decide on the action refresh or loadMore
     */
    private void handleAction() {
        if (pullRefreshEnable && refreshState != 2 && !isResetTrigger && moveDistance >= refreshTriggerDistance) {
            startRefresh(moveDistance, true);
        } else if (pullLoadMoreEnable && refreshState != 1 && !isResetTrigger && moveDistance <= -loadTriggerDistance) {
            startLoadMore(moveDistance, true);
        } else if ((refreshState == 0 && moveDistance > 0) || (refreshState == 1 && (moveDistance < 0 || isResetTrigger))) {
            resetHeaderView(moveDistance);
        } else if ((refreshState == 0 && moveDistance < 0) || (refreshState == 2 && moveDistance > 0) || isResetTrigger) {
            resetFootView(moveDistance);
        }
    }

    private void startRefresh(int headerViewHeight, final boolean withAction) {
        if (!isHoldingTrigger && onHeaderPullHolding()) {
            isHoldingTrigger = true;
        }
        if (!cancelAllAnimation(startRefreshAnimator)) {
            if (startRefreshAnimator == null) {
                startRefreshAnimator = getAnimator(headerViewHeight, refreshTriggerDistance, headerAnimationUpdate, refreshStartAnimationListener);
                startRefreshAnimator.setInterpolator(new DecelerateInterpolator(2f));
            } else {
                startRefreshAnimator.setIntValues(headerViewHeight, refreshTriggerDistance);
            }
            refreshWithAction = withAction;
            startRefreshAnimator.setDuration(getAnimationTime(headerViewHeight));
            startRefreshAnimator.start();
        }
    }

    private void resetHeaderView(int headerViewHeight) {
        if (headerViewHeight == 0) {
            resetHeaderAnimation.onAnimationStart(null);
            resetHeaderAnimation.onAnimationEnd(null);
            return;
        }
        if (!cancelAllAnimation(resetHeaderAnimator)) {
            if (resetHeaderAnimator == null) {
                resetHeaderAnimator = getAnimator(headerViewHeight, 0, headerAnimationUpdate, resetHeaderAnimation);
            } else {
                resetHeaderAnimator.setIntValues(headerViewHeight, 0);
            }
            resetHeaderAnimator.setDuration(getAnimationTime(headerViewHeight));
            resetHeaderAnimator.start();
        }
    }

    private void resetRefreshState() {
        if (isHoldingFinishTrigger && onHeaderPullReset()) ;
        if (footerView != null) {
            footerView.setVisibility(VISIBLE);
        }
        resetState();
    }

    private void startLoadMore(int loadMoreViewHeight, boolean withAction) {
        if (!isHoldingTrigger && onFooterPullHolding()) {
            isHoldingTrigger = true;
        }
        if (!cancelAllAnimation(startLoadMoreAnimator)) {
            if (startLoadMoreAnimator == null) {
                startLoadMoreAnimator = getAnimator(loadMoreViewHeight, -loadTriggerDistance, footerAnimationUpdate, loadingStartAnimationListener);
                startLoadMoreAnimator.setInterpolator(new DecelerateInterpolator(2f));
            } else {
                startLoadMoreAnimator.setIntValues(loadMoreViewHeight, -loadTriggerDistance);
            }
            refreshWithAction = withAction;
            startLoadMoreAnimator.setDuration(getAnimationTime(loadMoreViewHeight));
            startLoadMoreAnimator.start();
        }
    }

    private void resetFootView(int loadMoreViewHeight) {
        if (loadMoreViewHeight == 0) {
            resetFooterAnimationListener.onAnimationStart(null);
            resetFooterAnimationListener.onAnimationEnd(null);
            return;
        }
        if (!cancelAllAnimation(resetFooterAnimator)) {
            if (resetFooterAnimator == null) {
                resetFooterAnimator = getAnimator(loadMoreViewHeight, 0, footerAnimationUpdate, resetFooterAnimationListener);
            } else {
                resetFooterAnimator.setIntValues(loadMoreViewHeight, 0);
            }
            resetFooterAnimator.setDuration(getAnimationTime(loadMoreViewHeight));
            resetFooterAnimator.start();
        }
    }

    private void resetLoadMoreState() {
        if (isHoldingFinishTrigger && onFooterPullReset()) ;
        if (headerView != null) {
            headerView.setVisibility(VISIBLE);
        }
        resetState();
    }

    private void resetState() {
        isHoldingFinishTrigger = false;
        isAutoLoadingTrigger = false;
        isHoldingTrigger = false;
        pullStateControl = true;
        isResetTrigger = false;
        refreshState = 0;
    }

    private PullValueAnimator getAnimator(int firstValue, int secondValue, ValueAnimator.AnimatorUpdateListener updateListener, Animator.AnimatorListener animatorListener) {
        PullValueAnimator animator = new PullValueAnimator(firstValue, secondValue);
        animator.addUpdateListener(updateListener);
        animator.addListener(animatorListener);
        return animator;
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
        cancelAnimation(overScrollAnimator);
        cancelAnimation(startRefreshAnimator);
        cancelAnimation(resetHeaderAnimator);
        cancelAnimation(startLoadMoreAnimator);
        cancelAnimation(resetFooterAnimator);
        return false;
    }

    private long getAnimationTime(int moveDistance) {
        float ratio = Math.min(1, Math.abs((float) moveDistance / CommonUtils.getWindowHeight(getContext())));
        return (long) (Math.max((1.0f - Math.pow((1.0f - ratio), 100)) * animationDuring, animationDuring / 2));
    }

    private long getOverScrollTime(int moveDistance) {
        float ratio = Math.abs((float) moveDistance / CommonUtils.getWindowHeight(getContext()));
        return (long) ((Math.pow(2000 * ratio, 0.5)) * overScrollAdjustValue);
    }

    private int getFinalOverScrollDistance() {
        return scroller.getFinalY() - scroller.getCurrY();
    }

    private void dellNestedScrollCheck() {
        View target = targetView;
        while (target != pullContentLayout) {
            if (!(target instanceof NestedScrollingChild)) {
                isTargetNested = false;
                return;
            }
            target = (View) target.getParent();
        }
        isTargetNested = (target instanceof NestedScrollingChild);
    }

    private void removeDelayRunnable() {
        if (delayHandleActionRunnable != null) {
            removeCallbacks(delayHandleActionRunnable);
        }
    }

    private void setViewFront(boolean firstFront, boolean secondFront, View firstView, View secondView) {
        if (firstFront) {
            bringViewToFront(firstView);
        } else {
            bringViewToFront(pullContentLayout);
            if (secondFront) {
                bringViewToFront(secondView);
            }
        }
    }

    private void bringViewToFront(View view) {
        if (view != null) {
            view.bringToFront();
        }
    }

    /**
     * the fling may execute after onStopNestedScroll , so while overScrollBack try delay to handle action
     */
    private Runnable getDelayHandleActionRunnable() {
        return new Runnable() {
            public void run() {
                if (!pullTwinkEnable || (scroller != null && scroller.isFinished() && overScrollState == 0)) {
                    handleAction();
                }
            }
        };
    }

    private boolean nestedAble(View target) {
        return isTargetNestedScrollingEnabled() || !(target instanceof NestedScrollingChild);
    }

    /**
     * - use by generalHelper to dell touch logic
     *
     * @return
     */
    boolean isTargetNestedScrollingEnabled() {
        return isTargetNested && ViewCompat.isNestedScrollingEnabled(targetView);
    }

    private boolean flingAble() {
        return pullTwinkEnable || autoLoadingEnable;
    }

    private int overScrollFlingState() {
        if (moveDistance == 0) {
            return 0;
        }
        if (!generalPullHelper.isMovingDirectDown) {
            if (moveDistance > 0) {
                return 1;
            } else if (moveDistance < -loadTriggerDistance) {
                return -1;
            }
        } else {
            if (moveDistance < 0) {
                return 2;
            } else if (moveDistance > refreshTriggerDistance) {
                return -1;
            }
        }
        return 0;
    }

    private View getRefreshView(View v) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (v.getParent() != null) {
            ((ViewGroup) v.getParent()).removeView(v);
        }
        if (lp == null) {
            lp = new LayoutParams(-1, -2);
            v.setLayoutParams(lp);
        }
        return v;
    }

    private void onHeaderPullChange(float ratio) {
        if (headerView != null && headerView instanceof OnPullListener) {
            ((OnPullListener) headerView).onPullChange(ratio);
        }
    }

    private void onHeaderPullHoldTrigger() {
        if (headerView != null && headerView instanceof OnPullListener) {
            ((OnPullListener) headerView).onPullHoldTrigger();
        }
    }

    private void onHeaderPullHoldUnTrigger() {
        if (headerView != null && headerView instanceof OnPullListener) {
            ((OnPullListener) headerView).onPullHoldUnTrigger();
        }
    }

    private boolean onHeaderPullHolding() {
        if (headerView != null && headerView instanceof OnPullListener) {
            ((OnPullListener) headerView).onPullHolding();
            return true;
        }
        return false;
    }

    private boolean onHeaderPullFinish() {
        if (headerView != null && headerView instanceof OnPullListener) {
            ((OnPullListener) headerView).onPullFinish();
            return true;
        }
        return false;
    }

    private boolean onHeaderPullReset() {
        if (headerView != null && headerView instanceof OnPullListener) {
            ((OnPullListener) headerView).onPullReset();
            return true;
        }
        return false;
    }

    private void onFooterPullChange(float ratio) {
        if (footerView != null && footerView instanceof OnPullListener) {
            ((OnPullListener) footerView).onPullChange(ratio);
        }
    }

    private void onFooterPullHoldTrigger() {
        if (footerView != null && footerView instanceof OnPullListener) {
            ((OnPullListener) footerView).onPullHoldTrigger();
        }
    }

    private void onFooterPullHoldUnTrigger() {
        if (footerView != null && footerView instanceof OnPullListener) {
            ((OnPullListener) footerView).onPullHoldUnTrigger();
        }
    }

    private boolean onFooterPullHolding() {
        if (footerView != null && footerView instanceof OnPullListener) {
            ((OnPullListener) footerView).onPullHolding();
            return true;
        }
        return false;
    }

    private boolean onFooterPullFinish() {
        if (footerView != null && footerView instanceof OnPullListener) {
            ((OnPullListener) footerView).onPullFinish();
            return true;
        }
        return false;
    }

    private boolean onFooterPullReset() {
        if (footerView != null && footerView instanceof OnPullListener) {
            ((OnPullListener) footerView).onPullReset();
            return true;
        }
        return false;
    }

    public boolean isTargetAbleScrollUp() {
        return CommonUtils.canChildScrollUp(targetView);
    }

    public boolean isTargetAbleScrollDown() {
        return CommonUtils.canChildScrollDown(targetView);
    }

    /**
     * state animation
     */
    private final AnimatorListenerAdapter resetHeaderAnimation = new PullAnimatorListenerAdapter() {
        protected void animationStart() {
            if (isResetTrigger && refreshState == 1 && !isHoldingFinishTrigger && onHeaderPullFinish()) {
                isHoldingFinishTrigger = true;
            }
        }

        protected void animationEnd() {
            if (isResetTrigger) {
                resetRefreshState();
            }
        }
    };

    private final AnimatorListenerAdapter resetFooterAnimationListener = new PullAnimatorListenerAdapter() {
        protected void animationStart() {
            if (isResetTrigger && refreshState == 2 && !isHoldingFinishTrigger && onFooterPullFinish()) {
                isHoldingFinishTrigger = true;
            }
        }

        protected void animationEnd() {
            if (isResetTrigger) {
                resetLoadMoreState();
            }
        }
    };

    private final AnimatorListenerAdapter refreshStartAnimationListener = new PullAnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animation) {
            if (refreshState == 0) {
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

    private final AnimatorListenerAdapter loadingStartAnimationListener = new PullAnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animation) {
            if (refreshState == 0) {
                refreshState = 2;
                if (headerView != null) {
                    headerView.setVisibility(GONE);
                }
                if (onRefreshListener != null && refreshWithAction && !isAutoLoadingTrigger) {
                    onRefreshListener.onLoading();
                }
            }
        }
    };

    private final AnimatorListenerAdapter overScrollAnimatorListener = new PullAnimatorListenerAdapter() {
        public void onAnimationStart(Animator animation) {
            onNestedScrollAccepted(null, null, 2);
        }

        public void onAnimationEnd(Animator animation) {
            onStopScroll();
            onStopNestedScroll(null);
        }
    };

    /**
     * animator update listener
     */
    private final ValueAnimator.AnimatorUpdateListener headerAnimationUpdate = new ValueAnimator.AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator animation) {
            moveChildren((Integer) animation.getAnimatedValue());
            onHeaderPullChange((float) moveDistance / refreshTriggerDistance);
        }
    };

    private final ValueAnimator.AnimatorUpdateListener footerAnimationUpdate = new ValueAnimator.AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator animation) {
            moveChildren((Integer) animation.getAnimatedValue());
            onFooterPullChange((float) moveDistance / loadTriggerDistance);
        }
    };

    private final ValueAnimator.AnimatorUpdateListener overScrollAnimatorUpdate = new ValueAnimator.AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator animation) {
            int offsetY = (int) ((Integer) animation.getAnimatedValue() * overScrollDampingRatio);
            if (ViewCompat.isNestedScrollingEnabled(targetView)) {
                dispatchNestedScroll(0, 0, 0, offsetY, parentOffsetInWindow);
            }
            onScroll(offsetY + parentOffsetInWindow[1]);
        }
    };

    void onStartScroll() {
        abortScroller();
        cancelAllAnimation();
        overScrollState = 0;
        isOverScrollTrigger = false;
        isScrollAbleViewBackScroll = false;
    }

    void onPreScroll(int dy, int[] consumed) {
        if (dy > 0 && moveDistance > 0) {
            if (dy > moveDistance) {
                consumed[1] += moveDistance;
                dellScroll(-moveDistance);
                return;
            }
            consumed[1] += dy;
            dellScroll(-dy);
        } else if (dy < 0 && moveDistance < 0) {
            if (dy < moveDistance) {
                consumed[1] += moveDistance;
                dellScroll(-moveDistance);
                return;
            }
            dellScroll(-dy);
            consumed[1] += dy;
        }
    }

    void onScroll(int dy) {
        if ((generalPullHelper.isMovingDirectDown && !isTargetAbleScrollUp()) || (!generalPullHelper.isMovingDirectDown && !isTargetAbleScrollDown())) {
            if (dy < 0 && pullDownLimitDistance != 0 && moveDistance - dy > pullDownLimitDistance * dragDampingRatio) {
                dy = (int) (dy * (1 - (moveDistance / (float) pullDownLimitDistance)));
            } else if (dy > 0 && pullUpLimitDistance != 0 && -moveDistance + dy > pullUpLimitDistance * dragDampingRatio) {
                dy = (int) (dy * (1 - (-moveDistance / (float) pullUpLimitDistance)));
            } else {
                dy = (int) (dy * dragDampingRatio);
            }
            dellScroll(-dy);
        }
    }

    void onStopScroll() {
        removeDelayRunnable();
        if (!pullTwinkEnable) {
            handleAction();
        } else if ((overScrollFlingState() == 1 || overScrollFlingState() == 2) && !isOverScrollTrigger) {
            if (delayHandleActionRunnable == null) {
                delayHandleActionRunnable = getDelayHandleActionRunnable();
            }
            postDelayed(delayHandleActionRunnable, 50);
        } else if ((scroller != null && scroller.isFinished())) {
            handleAction();
        }
    }

    void onPreFling(float velocityY) {
        if (flingAble() && overScrollFlingState() != -1) {
            readyScroller();
            abortScroller();
            lastScrollY = 0;
            scroller.fling(0, 0, 0, (int) velocityY, 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
            finalScrollDistance = getFinalOverScrollDistance();
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        if (nestedAble(target)) {
            parentHelper.onNestedScrollAccepted(child, target, axes);
            startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
        }
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        if (nestedAble(target)) {
            if (isMoveWithContent) {
                onPreScroll(dy, consumed);
            }

            final int[] parentConsumed = parentScrollConsumed;
            if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
                consumed[0] += parentConsumed[0];
                consumed[1] += parentConsumed[1];
            }
        }
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        if (nestedAble(target)) {
            dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, parentOffsetInWindow);
            if (isMoveWithContent) {
                onScroll(dyUnconsumed + parentOffsetInWindow[1]);
            }
        }
    }

    @Override
    public void onStopNestedScroll(View child) {
        parentHelper.onStopNestedScroll(child);
        stopNestedScroll();
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        return dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        return dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public int getNestedScrollAxes() {
        return parentHelper.getNestedScrollAxes();
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
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
        return childHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return childHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return childHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    public static class LayoutParams extends MarginLayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        @SuppressWarnings({"unused"})
        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
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

    private final class PullValueAnimator extends ValueAnimator {
        PullValueAnimator(int... values) {
            setIntValues(values);
        }

        public void start() {
            if (isAttachWindow) {
                super.start();
            }
        }
    }

    private class PullAnimatorListenerAdapter extends AnimatorListenerAdapter {
        boolean isCancel;

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

    /**
     * ------------------| open api |------------------
     */

    /**
     * move children
     */
    public final void moveChildren(int distance) {
        moveDistance = distance;
        dellAutoLoading();
        if (isMoveWithFooter) {
            showGravity.dellFooterMoving(moveDistance);
        }
        if (isMoveWithHeader) {
            showGravity.dellHeaderMoving(moveDistance);
        }
        if (isMoveWithContent) {
            ViewCompat.setTranslationY(pullContentLayout, moveDistance);
        }
    }

    public final boolean cancelAllAnimation() {
        return cancelAllAnimation(null);
    }

    public void refreshComplete() {
        if (refreshState != 2) {
            isResetTrigger = true;
            resetHeaderView(moveDistance);
        }
    }

    public void loadMoreComplete() {
        if (refreshState != 1) {
            isResetTrigger = true;
            resetFootView(moveDistance);
        }
    }

    public void autoLoading() {
        autoLoading(true);
    }

    public void autoLoading(boolean withAction) {
        if (refreshState != 0 || pullContentLayout == null || !pullLoadMoreEnable) {
            return;
        }
        startLoadMore(moveDistance, withAction);
    }

    public void autoRefresh() {
        autoRefresh(true);
    }

    public void autoRefresh(boolean withAction) {
        if (refreshState != 2 && pullContentLayout != null && pullRefreshEnable) {
            cancelAllAnimation();
            resetState();
            startRefresh(moveDistance, withAction);
        }
    }

    public void setHeaderView(View header) {
        if (headerView != null && headerView != header) {
            removeView(headerView);
        }
        headerView = header;
        if (header == null) {
            return;
        }
        addView(getRefreshView(header));

        if (!isHeaderFront) {
            setViewFront(false, isFooterFront, null, footerView);
        }
    }

    public void setFooterView(View footer) {
        if (footerView != null && footerView != footer) {
            removeView(footerView);
        }
        footerView = footer;
        if (footer == null) {
            return;
        }
        addView(getRefreshView(footer));

        if (!isFooterFront) {
            setViewFront(false, isHeaderFront, null, headerView);
        }
    }

    public void setTargetView(View targetView) {
        this.targetView = targetView;
        cancelTouchEvent();
        dellNestedScrollCheck();
        if ((targetView instanceof RecyclerView) && flingAble()) {
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

    public void setPullDownLimitDistance(int pullDownLimitDistance) {
        this.pullDownLimitDistance = pullDownLimitDistance;
    }

    public void setPullUpLimitDistance(int pullUpLimitDistance) {
        this.pullUpLimitDistance = pullUpLimitDistance;
    }

    public void setOverScrollAdjustValue(float overScrollAdjustValue) {
        this.overScrollAdjustValue = overScrollAdjustValue;
    }

    public void setOverScrollMaxTriggerOffset(int overScrollMaxTriggerOffset) {
        this.overScrollMaxTriggerOffset = overScrollMaxTriggerOffset;
    }

    public void setOverScrollDampingRatio(float overScrollDampingRatio) {
        this.overScrollDampingRatio = overScrollDampingRatio;
    }

    public void setAnimationDuring(int animationDuring) {
        this.animationDuring = animationDuring;
    }

    public void setDragDampingRatio(float dragDampingRatio) {
        this.dragDampingRatio = dragDampingRatio;
    }

    public void setAutoLoadingEnable(boolean ableAutoLoading) {
        autoLoadingEnable = ableAutoLoading;
    }

    public void setRefreshShowGravity(@ShowGravity.ShowState int headerShowGravity
            , @ShowGravity.ShowState int footerShowGravity) {
        setHeaderShowGravity(headerShowGravity);
        setFooterShowGravity(footerShowGravity);
    }

    public void setHeaderShowGravity(@ShowGravity.ShowState int headerShowGravity) {
        showGravity.headerShowGravity = headerShowGravity;
        requestLayout();
    }

    public void setFooterShowGravity(@ShowGravity.ShowState int footerShowGravity) {
        showGravity.footerShowGravity = footerShowGravity;
        requestLayout();
    }

    public void setHeaderFront(boolean headerFront) {
        if (isHeaderFront != headerFront) {
            isHeaderFront = headerFront;
            setViewFront(isHeaderFront, isFooterFront, headerView, footerView);
        }
    }

    public void setFooterFront(boolean footerFront) {
        if (isFooterFront != footerFront) {
            isFooterFront = footerFront;
            setViewFront(isFooterFront, isHeaderFront, footerView, headerView);
        }
    }

    public void setMoveWithFooter(boolean moveWithFooter) {
        this.isMoveWithFooter = moveWithFooter;
    }

    public void setMoveWithContent(boolean moveWithContent) {
        this.isMoveWithContent = moveWithContent;
    }

    public void setMoveWithHeader(boolean moveWithHeader) {
        this.isMoveWithHeader = moveWithHeader;
    }

    public final void cancelTouchEvent() {
        if (generalPullHelper.dragState != 0) {
            dispatchTouchEvent(MotionEvent.obtain(System.currentTimeMillis()
                    , System.currentTimeMillis(), MotionEvent.ACTION_CANCEL, 0, 0, 0));
        }
    }

    public void setDispatchPullTouchAble(boolean dispatchPullTouchAble) {
        this.dispatchPullTouchAble = dispatchPullTouchAble;
    }

    public final int getMoveDistance() {
        return moveDistance;
    }

    public int getRefreshTriggerDistance() {
        return refreshTriggerDistance;
    }

    public int getLoadTriggerDistance() {
        return loadTriggerDistance;
    }

    public boolean isOverScrollUp() {
        return overScrollState == 1;
    }

    public boolean isOverScrollDown() {
        return overScrollState == 2;
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
        return refreshState == 0 && startRefreshAnimator != null && startRefreshAnimator.isRunning() || refreshState == 1;
    }

    public boolean isLoading() {
        return refreshState == 0 && startLoadMoreAnimator != null && startLoadMoreAnimator.isRunning() || refreshState == 2;
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

    public boolean isHoldingTrigger() {
        return isHoldingTrigger;
    }

    public boolean isHoldingFinishTrigger() {
        return isHoldingFinishTrigger;
    }

    public boolean isOverScrollTrigger() {
        return isOverScrollTrigger;
    }

    public boolean isAutoLoadingTrigger() {
        return isAutoLoadingTrigger;
    }

}