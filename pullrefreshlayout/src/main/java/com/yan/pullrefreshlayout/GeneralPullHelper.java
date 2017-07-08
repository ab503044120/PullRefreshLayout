package com.yan.pullrefreshlayout;

import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;

/**
 * support general view to pull refresh
 * Created by yan on 2017/6/29.
 */

class GeneralPullHelper {
    private static final String TAG = "GeneralPullHelper";
    private PullRefreshLayout pullRefreshLayout;

    /**
     * last motion point y
     */
    private int lastMotionPointY;

    /**
     * is last motion point y set
     */
    private boolean isLastMotionPointYSet;

    /**
     * first touch point x
     */
    private float actionDownPointX;

    /**
     * first touch point y
     */
    private float actionDownPointY;

    /**
     * first touch moving point x
     */
    private float movingPointX;

    /**
     * first touch moving point y
     */
    private float movingPointY;

    /**
     * is touch direct down
     */
    private boolean isDragDown;

    /**
     * motionEvent consumed
     */
    private int[] consumed = new int[2];

    /**
     * dell the interceptTouchEvent
     */
    private int interceptTouchCount = 0;
    private int interceptTouchLastCount = -1;

    /**
     * touchEvent velocityTracker
     */
    private VelocityTracker velocityTracker;

    /**
     * velocity y
     */
    private float velocityY;

    /**
     * first touch event id , to dell the different finger touch event
     */
    private int firstTouchEventId;

    GeneralPullHelper(PullRefreshLayout pullRefreshLayout) {
        this.pullRefreshLayout = pullRefreshLayout;
    }

    boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                onTouchEvent(ev);
                initVelocityTracker(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                velocityTrackerCompute(ev);
                if (interceptTouchLastCount != interceptTouchCount) {
                    interceptTouchLastCount = interceptTouchCount;
                } else if (Math.abs(movingPointY - actionDownPointY) > Math.abs(movingPointX - actionDownPointX)
                        || (pullRefreshLayout.moveDistance != 0)) {
                    if (!isLastMotionPointYSet) {
                        isLastMotionPointYSet = true;
                        lastMotionPointY = (int) ev.getY();
                    }
                    onTouchEvent(ev);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                onTouchEvent(ev);
                cancelVelocityTracker();
                velocityY = 0;
                interceptTouchLastCount = -1;
                interceptTouchCount = 0;
                isLastMotionPointYSet = false;
                break;
        }
        return false;
    }

    boolean onInterceptTouchEvent(MotionEvent ev) {
        if (pullRefreshLayout.moveDistance != 0) {
            return true;
        }
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                actionDownPointX = ev.getX();
                actionDownPointY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                interceptTouchCount++;
                movingPointX = ev.getX();
                movingPointY = ev.getY();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                break;
        }

        return false;
    }

    boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                firstTouchEventId = event.getPointerId(0);
                pullRefreshLayout.onNestedScrollAccepted(pullRefreshLayout.targetView, pullRefreshLayout.targetView, 2);
                pullRefreshLayout.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                pullRefreshLayout.onStartNestedScroll(pullRefreshLayout.targetView, pullRefreshLayout.targetView, 2);
                lastMotionPointY = (int) event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (firstTouchEventId != event.getPointerId(0)) {
                    lastMotionPointY = (int) event.getY();
                    firstTouchEventId = event.getPointerId(0);
                }
                int tempPointX = (int) event.getX();
                int tempPointY = (int) event.getY();
                int tempOffsetY = tempPointY - lastMotionPointY;
                if (tempOffsetY > 0) {
                    isDragDown = true;
                } else if (tempOffsetY < 0) {
                    isDragDown = false;
                }
                if ((isDragDown && !pullRefreshLayout.canChildScrollUp())
                        || (!isDragDown && !pullRefreshLayout.canChildScrollDown())) {
                    pullRefreshLayout.onNestedScroll(null, 0, 0, 0, -tempOffsetY);
                } else if (pullRefreshLayout.moveDistance > 0 && !isDragDown) {
                    pullRefreshLayout.onNestedPreScroll(null, 0, -tempOffsetY, consumed);
                } else if (pullRefreshLayout.moveDistance < 0 && isDragDown) {
                    pullRefreshLayout.onNestedPreScroll(null, 0, -tempOffsetY, consumed);
                }
                lastMotionPointY = tempPointY;
                event.setLocation(tempPointX + consumed[0], tempPointY + consumed[1]);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                consumed[0] = 0;
                consumed[1] = 0;
                if (isLastMotionPointYSet) {
                    if (!pullRefreshLayout.dispatchNestedPreFling(0, -velocityY)) {
                        pullRefreshLayout.onNestedPreFling(pullRefreshLayout.targetView, 0, -velocityY);
                    }
                }
                pullRefreshLayout.onStopNestedScroll(pullRefreshLayout.targetView);
                break;
        }
        return true;
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
            velocityTracker.computeCurrentVelocity(1000);
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

    void autoRefreshDell() {
        movingPointY = 100;
        actionDownPointY = 0;
        movingPointX = 0;
        actionDownPointX = 0;
    }

}
