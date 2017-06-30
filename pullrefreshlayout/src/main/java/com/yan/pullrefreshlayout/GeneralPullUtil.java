package com.yan.pullrefreshlayout;

import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;

/**
 * support general view to pull refresh
 * Created by yan on 2017/6/29.
 */

class GeneralPullUtil {
    private static final String TAG = "CommonViewSupport";
    private PullRefreshLayout pullRefreshLayout;
    private float lastMotionPointY;
    private boolean isLastMotionPointYSet;
    private float actionDownPointX;
    private float actionDownPointY;
    private float movingPointX;
    private float movingPointY;
    private boolean isDragDown;

    private int[] consumed = new int[2];

    private int interceptTouchCount = 0;
    private int interceptTouchLastCount = -1;

    private VelocityTracker velocityTracker;

    private float velocityY;

    GeneralPullUtil(PullRefreshLayout pullRefreshLayout) {
        this.pullRefreshLayout = pullRefreshLayout;
    }

    boolean dispatchTouchEvent(MotionEvent ev) {
        if (!pullRefreshLayout.canChildScrollDown() && !pullRefreshLayout.canChildScrollUp()) {
            return false;
        }
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                onTouchEvent(ev);
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain();
                }
                velocityTracker.addMovement(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                try {
                    velocityTracker.addMovement(ev);
                    velocityTracker.computeCurrentVelocity(1000);
                    velocityY = velocityTracker.getYVelocity();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (interceptTouchLastCount != interceptTouchCount) {
                    interceptTouchLastCount = interceptTouchCount;
                } else {
                    if (Math.abs(movingPointY - actionDownPointY) > Math.abs(movingPointX - actionDownPointX)
                            || (pullRefreshLayout.moveDistance != 0)) {
                        if (!isLastMotionPointYSet) {
                            isLastMotionPointYSet = true;
                            lastMotionPointY = ev.getY();
                        }
                        onTouchEvent(ev);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                onTouchEvent(ev);
                try {
                    velocityTracker.clear();
                    velocityTracker.recycle();
                    velocityTracker = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                interceptTouchLastCount = -1;
                interceptTouchCount = 0;
                isLastMotionPointYSet = false;
                break;
        }
        return false;
    }

    boolean onInterceptTouchEvent(MotionEvent ev) {
        if (pullRefreshLayout.moveDistance != 0) return true;
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
                pullRefreshLayout.onStartNestedScroll(null, null, 0);
                lastMotionPointY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float tempPointX = event.getX();
                float tempPointY = event.getY();
                float tempOffsetY = tempPointY - lastMotionPointY;
                if (tempOffsetY > 0) {
                    isDragDown = true;
                } else if (tempOffsetY < 0) {
                    isDragDown = false;
                }
                if ((isDragDown && !pullRefreshLayout.canChildScrollUp())
                        || (!isDragDown && !pullRefreshLayout.canChildScrollDown())) {
                    pullRefreshLayout.onNestedScroll(null, 0, 0, 0, -(int) tempOffsetY);

                } else if (pullRefreshLayout.moveDistance > 0 && !isDragDown) {
                    pullRefreshLayout.onNestedPreScroll(null, 0, -(int) tempOffsetY, consumed);
                } else if (pullRefreshLayout.moveDistance < 0 && isDragDown) {
                    pullRefreshLayout.onNestedPreScroll(null, 0, -(int) tempOffsetY, consumed);
                }
                lastMotionPointY = tempPointY;

                event.setLocation(tempPointX + consumed[0], tempPointY + consumed[1]);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                consumed[0] = 0;
                consumed[1] = 0;
                if (isLastMotionPointYSet) {
                    pullRefreshLayout.onNestedPreFling(null, 0, -velocityY);
                }
                pullRefreshLayout.onStopNestedScroll(null);
                velocityY = 0;
                break;
        }
        return true;
    }
}
