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
    private boolean isTriggerMoveEvent;

    /**
     * is moving direct down
     * - use by pullRefreshLayout to get moving direction
     */
    boolean isMovingDirectDown;

    /**
     * is touch direct down
     * - use by pullRefreshLayout to get drag state
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
     * moveOffsetY
     */
    private int moveOffsetY;

    /**
     * active pointer id
     */
    private int activePointerId;

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

    void dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dellTouchEvent(ev);
                initVelocityTracker(ev);
                actionDownPointX = ev.getX();
                actionDownPointY = ev.getY();
                lastTouchY = ev.getY();

                lastMotionY = (int) ev.getY();
                activePointerId = ev.getPointerId(0);
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

                if (isTriggerMoveEvent) {
                    dellTouchEvent(ev);
                    return;
                }

                float movingX = ev.getX() - actionDownPointX;
                float movingY = ev.getY() - actionDownPointY;
                if (((Math.sqrt(movingY * movingY + movingX * movingX) > touchSlop && Math.abs(movingY) > Math.abs(movingX)) || pullRefreshLayout.moveDistance != 0)) {
                    isTriggerMoveEvent = true;
                    lastMotionY = (int) ev.getY();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dellTouchEvent(ev);
                cancelVelocityTracker();
                isTriggerMoveEvent = false;
                velocityY = 0;
                dragState = 0;
                break;
        }
    }

    private void dellTouchEvent(MotionEvent ev) {
        int actionMasked = MotionEventCompat.getActionMasked(ev);
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN: {
                pullRefreshLayout.onStartScroll();
                break;
            }
            case MotionEvent.ACTION_MOVE:
                if (!pullRefreshLayout.nestedScrollAble || !pullRefreshLayout.isMoveWithContent) {
                    if (activePointerId != ev.getPointerId(0)) {
                        lastMotionY = (int) ev.getY();
                        activePointerId = ev.getPointerId(0);
                    }

                    // --------------------| move offset |--------------------
                    final int y = (int) ev.getY();
                    int deltaY = lastMotionY - y;
                    lastMotionY = y;

                    pullRefreshLayout.onPreScroll(deltaY, childConsumed);

                    // ------------------| consume offset |-------------------
                    int deltaYOffset = childConsumed[1] - lastChildConsumedY;
                    lastChildConsumedY = childConsumed[1];

                    pullRefreshLayout.onScroll(deltaY - deltaYOffset);

                    // -------------------| event reset |--------------------
                    if (!pullRefreshLayout.isMoveWithContent) {
                        ev.offsetLocation(0, moveOffsetY = childConsumed[1]);
                        ev.setLocation((int) ev.getX(), (int) ev.getY());
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                ev.offsetLocation(0, moveOffsetY);

                pullRefreshLayout.onStopScroll();
                if (isTriggerMoveEvent && (Math.abs(velocityY) > minimumFlingVelocity)) {
                    pullRefreshLayout.onPreFling(-(int) velocityY);
                }

                moveOffsetY = 0;
                activePointerId = -1;
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
