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

    boolean dispatchTouchEvent(MotionEvent ev, MotionEvent[] finalMotionEvent) {
        finalMotionEvent[0] = ev;

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dellTouchEvent(ev);
                initVelocityTracker(ev);
                actionDownPointX = ev.getX();
                actionDownPointY = ev.getY();
                lastTouchY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                /**
                 * director dell
                 */
                float tempY = ev.getY();
                if (tempY - lastTouchY > 0) {
                    dragState = 1;
                    isMovingDirectDown = true;
                } else if (tempY - lastTouchY < 0) {
                    dragState = -1;
                    isMovingDirectDown = false;
                }
                lastTouchY = tempY;

                /**
                 * touch logic
                 */
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
                dragState = 0;
                break;
        }
        return false;
    }

    private void dellTouchEvent(MotionEvent ev) {
        if (pullRefreshLayout.nestedScrollAble) {
            return;
        }

        final int actionMasked = MotionEventCompat.getActionMasked(ev);
        if (actionMasked == MotionEvent.ACTION_DOWN) {
            nestedYOffset = 0;
        }
        ev.offsetLocation(0, nestedYOffset);
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN: {

                lastMotionY = (int) ev.getY();
                activePointerId = ev.getPointerId(0);
                pullRefreshLayout.onStartScroll();
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

                pullRefreshLayout.onPreScroll(deltaY, childConsumed);

                int deltaYOffset = childConsumed[1] - lastChildConsumedY;
                pullRefreshLayout.onScroll(deltaY - deltaYOffset);

                ev.offsetLocation(0, deltaYOffset);
                lastChildConsumedY = childConsumed[1];
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                pullRefreshLayout.onStopScroll();
                if (isLastMotionYSet && (Math.abs(velocityY) > minimumFlingVelocity)) {
                    pullRefreshLayout.onPreFling(-(int) velocityY);
                }
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
