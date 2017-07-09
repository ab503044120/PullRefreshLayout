package com.yan.pullrefreshlayout;

import android.support.v4.view.MotionEventCompat;
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
     * motionEvent childConsumed
     */
    private int[] childConsumed = new int[2];

    /**
     * motionEvent last Child Consumed Y
     */
    private int lastChildConsumedY;

    /**
     * active Pointer Id
     */
    private int activePointerId;

    /**
     * last Motion Y
     */
    private int lastMotionY;

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

    GeneralPullHelper(PullRefreshLayout pullRefreshLayout) {
        this.pullRefreshLayout = pullRefreshLayout;
    }

    boolean dispatchTouchEvent(MotionEvent ev, MotionEvent[] finalMotionEvent) {
        finalMotionEvent[0] = ev;
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
                        lastMotionY = (int) ev.getY();
                    }
                    onTouchEvent(ev);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                onTouchEvent(ev);
                cancelVelocityTracker();
                isLastMotionPointYSet = false;
                velocityY = 0;
                interceptTouchLastCount = -1;
                interceptTouchCount = 0;
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

    boolean onTouchEvent(MotionEvent ev) {
        MotionEvent vtev = MotionEvent.obtain(ev);
        final int actionMasked = MotionEventCompat.getActionMasked(ev);
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN: {
                lastMotionY = (int) ev.getY();
                activePointerId = ev.getPointerId(0);
                break;
            }
            case MotionEvent.ACTION_MOVE:
                if (activePointerId != vtev.getPointerId(0)) {
                    lastMotionY = (int) vtev.getY();
                    activePointerId = vtev.getPointerId(0);
                }
                final int y = (int) ev.getY();
                int deltaY = lastMotionY - y;
                lastMotionY = y;
                if (deltaY < 0) {
                    isDragDown = true;
                } else if (deltaY > 0) {
                    isDragDown = false;
                }
                if ((isDragDown && !pullRefreshLayout.canChildScrollUp())
                        || (!isDragDown && !pullRefreshLayout.canChildScrollDown())) {
                    pullRefreshLayout.onNestedScroll(null, 0, 0, 0, deltaY);
                } else if ((pullRefreshLayout.moveDistance < 0 && isDragDown)
                        || (pullRefreshLayout.moveDistance > 0 && !isDragDown)) {
                    pullRefreshLayout.onNestedPreScroll(null, 0, deltaY, childConsumed);
                    vtev.offsetLocation(0, childConsumed[1] - lastChildConsumedY);
                    lastChildConsumedY = childConsumed[1];
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isLastMotionPointYSet) {
                    if (!pullRefreshLayout.dispatchNestedPreFling(0, -velocityY)) {
                        pullRefreshLayout.onNestedPreFling(pullRefreshLayout.targetView, 0, -velocityY);
                    }
                }
                pullRefreshLayout.onStopNestedScroll(pullRefreshLayout.targetView);
                lastChildConsumedY = 0;
                break;
        }
        vtev.recycle();
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
