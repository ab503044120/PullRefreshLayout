package com.yan.pullrefreshlayout;

import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.ViewCompat;
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
     * is touch final direct down
     */
    private boolean isConsumedDragDown;

    /**
     * is moving direct down
     */
     boolean isMovingDirectDown;

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
    int isDragDown;
    /**
     * motionEvent childConsumed
     */
    private int[] childConsumed = new int[2];
    private int lastChildConsumedY;

    /**
     * active Pointer Id
     */
    private int activePointerId;

    /**
     * nested Y Offset
     */
    private int nestedYOffset;

    /**
     * last motion y
     */
    private int lastMotionY;

    /**
     * last motion y
     */
    private float lastTouchY;

    /**
     * scroll Consumed Offset
     */
    private int[] scrollConsumed = new int[2];
    private int[] scrollOffset = new int[2];

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

    void dellDirection(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            lastTouchY = event.getY();
            return;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            float tempY = event.getY();
            if (tempY - lastTouchY > 0) {
                isDragDown = 1;
                isMovingDirectDown = true;
            } else if (tempY - lastTouchY < 0) {
                isDragDown = -1;
                isMovingDirectDown = false;
            }
            lastTouchY = tempY;
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            isDragDown = 0;
        }
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

    public boolean onTouchEvent(MotionEvent ev) {
        MotionEvent vtev = MotionEvent.obtain(ev);

        final int actionMasked = MotionEventCompat.getActionMasked(ev);

        if (actionMasked == MotionEvent.ACTION_DOWN) {
            nestedYOffset = 0;
        }
        vtev.offsetLocation(0, nestedYOffset);
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN: {
                lastMotionY = (int) ev.getY();
                activePointerId = ev.getPointerId(0);
                pullRefreshLayout.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                break;
            }
            case MotionEvent.ACTION_MOVE:
                if (activePointerId != vtev.getPointerId(0)) {
                    lastMotionY = (int) vtev.getY();
                    activePointerId = vtev.getPointerId(0);
                }

                final int y = (int) ev.getY();
                int deltaY = lastMotionY - y;
                if (pullRefreshLayout.dispatchNestedPreScroll(0, deltaY, scrollConsumed, scrollOffset)) {
                    deltaY -= scrollConsumed[1];
                    vtev.offsetLocation(0, scrollOffset[1]);
                    nestedYOffset += scrollOffset[1];
                }
                lastMotionY = y - scrollOffset[1];
                final int oldY = pullRefreshLayout.targetView.getScrollY();

                if (deltaY < 0) {
                    isConsumedDragDown = true;
                } else if (deltaY > 0) {
                    isConsumedDragDown = false;
                }
                if ((pullRefreshLayout.moveDistance < 0 && isConsumedDragDown)
                        || (pullRefreshLayout.moveDistance > 0 && !isConsumedDragDown)) {
                    pullRefreshLayout.onNestedPreScroll(null, 0, deltaY, childConsumed);
                    vtev.offsetLocation(0, childConsumed[1] - lastChildConsumedY);
                    lastChildConsumedY = childConsumed[1];
                    return true;
                }

                final int scrolledDeltaY = pullRefreshLayout.targetView.getScrollY() - oldY;
                final int unconsumedY = deltaY - scrolledDeltaY;

                if (pullRefreshLayout.dispatchNestedScroll(0, 0
                        , (pullRefreshLayout.canChildScrollUp() && pullRefreshLayout.canChildScrollDown() && pullRefreshLayout.moveDistance == 0 ? deltaY : 0)
                        , ((isConsumedDragDown && !pullRefreshLayout.canChildScrollUp())
                                || (!isConsumedDragDown && !pullRefreshLayout.canChildScrollDown())) ? unconsumedY : 0
                        , scrollOffset)) {
                    lastMotionY -= scrollOffset[1];
                    vtev.offsetLocation(0, scrollOffset[1]);
                    nestedYOffset += scrollOffset[1];

                }
                if ((isConsumedDragDown && !pullRefreshLayout.canChildScrollUp())
                        || (!isConsumedDragDown && !pullRefreshLayout.canChildScrollDown())) {
                    pullRefreshLayout.onNestedScroll(null, 0, 0, 0, scrollOffset[1] == 0 ? deltaY : 0);
                }

                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isLastMotionPointYSet) {
                    flingWithNestedDispatch(-(int) velocityY);
                }
                pullRefreshLayout.onStopNestedScroll(pullRefreshLayout.targetView);
                activePointerId = -1;
                childConsumed[0] = 0;
                childConsumed[1] = 0;
                lastChildConsumedY = 0;
                scrollOffset[1] = 0;
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

    void flingWithNestedDispatch(int velocityY) {
        if (!pullRefreshLayout.dispatchNestedPreFling(0, velocityY)) {
            pullRefreshLayout.dispatchNestedFling(0, velocityY, true);
            pullRefreshLayout.onNestedPreFling(pullRefreshLayout.targetView, 0, velocityY);
        }
    }

}
