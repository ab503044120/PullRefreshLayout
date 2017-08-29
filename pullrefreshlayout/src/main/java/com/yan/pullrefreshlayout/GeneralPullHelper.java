package com.yan.pullrefreshlayout;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

/**
 * support general view to pull refresh
 * Created by yan on 2017/6/29.
 */
class GeneralPullHelper {
    private static final String TAG = "GeneralPullHelper";
    private final PullRefreshLayout pullRefreshLayout;

    /**
     * default values
     */
    private final int minimumFlingVelocity;
    private final int maximumVelocity;
    private final float touchSlop;

    /**
     * is last motion point y set
     */
    private boolean isLastMotionYSet;

    /**
     * is moving direct down
     */
    boolean isMovingDirectDown;

    /**
     * is touch direct down
     */
    int dragState;

    /**
     * first touch point x
     */
    private float actionDownPointX;

    /**
     * first touch point y
     */
    private float actionDownPointY;

    /**
     * motion event child consumed
     */
    private int[] childConsumed = new int[2];
    private int lastChildConsumedY;

    /**
     * active pointer id
     */
    private int activePointerId;

    /**
     * nested y offset
     */
    private int nestedYOffset;

    /**
     * last motion y
     */
    private int lastMotionY;

    /**
     * last touch y
     */
    private float lastTouchY;

    /**
     * touchEvent velocityTracker
     */
    private VelocityTracker velocityTracker;

    /**
     * velocity y
     */
    private float velocityY;

    GeneralPullHelper(PullRefreshLayout pullRefreshLayout, Context context) {
        this.pullRefreshLayout = pullRefreshLayout;
        ViewConfiguration configuration = ViewConfiguration.get(context);
        minimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        maximumVelocity = configuration.getScaledMaximumFlingVelocity();
        touchSlop = configuration.getScaledTouchSlop();
    }

    private void dellDirection(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            lastTouchY = event.getY();
            return;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            float tempY = event.getY();
            if (tempY - lastTouchY > 0) {
                dragState = 1;
                isMovingDirectDown = true;
            } else if (tempY - lastTouchY < 0) {
                dragState = -1;
                isMovingDirectDown = false;
            }
            lastTouchY = tempY;
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            dragState = 0;
        }
    }

    boolean dispatchTouchEvent(MotionEvent ev, MotionEvent[] finalMotionEvent) {
        dellDirection(ev);
        finalMotionEvent[0] = ev;

        if (pullRefreshLayout.nestedScrollAble) {
            if ((ev.getActionMasked() == MotionEvent.ACTION_UP || ev.getActionMasked() == MotionEvent.ACTION_CANCEL)) {
                pullRefreshLayout.onStopNestedScroll(pullRefreshLayout.targetView);
            }
            return false;
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dellTouchEvent(ev);
                initVelocityTracker(ev);
                actionDownPointX = ev.getX();
                actionDownPointY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                velocityTrackerCompute(ev);
                float movingX = ev.getX() - actionDownPointX;
                float movingY = ev.getY() - actionDownPointY;
                if ((Math.sqrt(movingY * movingY + movingX * movingX) > touchSlop
                        && Math.abs(movingY) > Math.abs(movingX))
                        || pullRefreshLayout.moveDistance != 0) {
                    if (!isLastMotionYSet) {
                        isLastMotionYSet = true;
                        lastMotionY = (int) ev.getY();
                    }
                    dellTouchEvent(ev);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dellTouchEvent(ev);
                cancelVelocityTracker();
                velocityY = 0;
                isLastMotionYSet = false;
                break;
        }
        return false;
    }

    private void dellTouchEvent(MotionEvent ev) {
        final int actionMasked = MotionEventCompat.getActionMasked(ev);
        if (actionMasked == MotionEvent.ACTION_DOWN) {
            nestedYOffset = 0;
        }
        ev.offsetLocation(0, nestedYOffset);
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN: {

                lastMotionY = (int) ev.getY();
                activePointerId = ev.getPointerId(0);
                pullRefreshLayout.onStartNestedScroll(null, null, 2);
                pullRefreshLayout.onNestedScrollAccepted(null, null, 2);
                break;
            }
            case MotionEvent.ACTION_MOVE:
                if (activePointerId != ev.getPointerId(0)) {
                    lastMotionY = (int) ev.getY();
                    activePointerId = ev.getPointerId(0);
                }

                final int y = (int) ev.getY();
                int deltaY = lastMotionY - y;
                lastMotionY = y;

                pullRefreshLayout.onNestedPreScroll(null, 0, deltaY, childConsumed);

                int deltaYOffset = childConsumed[1] - lastChildConsumedY;
                pullRefreshLayout.onNestedScroll(null, 0, 0, 0, deltaY - deltaYOffset);

                ev.offsetLocation(0, deltaYOffset);
                lastChildConsumedY = childConsumed[1];
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isLastMotionYSet && (Math.abs(velocityY) > minimumFlingVelocity)) {
                    pullRefreshLayout.onNestedPreFling(null, 0, -(int) velocityY);
                }
                pullRefreshLayout.onStopNestedScroll(null);
                activePointerId = -1;
                childConsumed[0] = 0;
                childConsumed[1] = 0;
                lastChildConsumedY = 0;
                break;
        }

    }

    /**
     * velocityTracker dell
     *
     * @param ev MotionEvent
     */
    private void initVelocityTracker(MotionEvent ev) {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(ev);
    }

    private void velocityTrackerCompute(MotionEvent ev) {
        try {
            velocityTracker.addMovement(ev);
            velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
            velocityY = velocityTracker.getYVelocity();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cancelVelocityTracker() {
        try {
            velocityTracker.clear();
            velocityTracker.recycle();
            velocityTracker = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
