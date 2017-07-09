//package com.yan.pullrefreshlayout;
//
//import android.support.v4.view.MotionEventCompat;
//import android.support.v4.view.ViewCompat;
//import android.support.v4.view.ViewConfigurationCompat;
//import android.util.Log;
//import android.view.MotionEvent;
//import android.view.VelocityTracker;
//import android.view.View;
//import android.view.ViewConfiguration;
//import android.view.ViewParent;
//
//import java.util.Arrays;
//
///**
// * support general view to pull refresh
// * Created by yan on 2017/6/29.
// */
//
//class GeneralPullHelper {
//    private static final String TAG = "GeneralPullHelper";
//    private PullRefreshLayout pullRefreshLayout;
//
//    /**
//     * last motion point y
//     */
//
//    /**
//     * is last motion point y set
//     */
//    private boolean isLastMotionPointYSet;
//
//    /**
//     * first touch point x
//     */
//    private float actionDownPointX;
//
//    /**
//     * first touch point y
//     */
//    private float actionDownPointY;
//
//    /**
//     * first touch moving point x
//     */
//    private float movingPointX;
//
//    /**
//     * first touch moving point y
//     */
//    private float movingPointY;
//
//    /**
//     * is touch direct down
//     */
//    private boolean isDragDown;
//
//    /**
//     * motionEvent childConsumed
//     */
//    private int[] childConsumed = new int[2];
//    private int lastChildConsumedY;
//
//    private int mActivePointerId;
//    private int mNestedYOffset;
//    private int mLastMotionY;
//    private int[] mScrollConsumed = new int[2];
//    private int[] mScrollOffset = new int[2];
//    private boolean mIsBeingDragged;
//
//    /**
//     * dell the interceptTouchEvent
//     */
//    private int interceptTouchCount = 0;
//    private int interceptTouchLastCount = -1;
//
//    /**
//     * touchEvent velocityTracker
//     */
//    private VelocityTracker velocityTracker;
//
//    /**
//     * velocity y
//     */
//    private float velocityY;
//
//
//    GeneralPullHelper(PullRefreshLayout pullRefreshLayout) {
//        this.pullRefreshLayout = pullRefreshLayout;
//    }
//
//    boolean isMoving;
//    boolean resetEvent;
//
//    boolean dispatchTouchEvent(MotionEvent ev, MotionEvent[] finalMotionEvent) {
//        finalMotionEvent[0] = ev;
//        switch (ev.getActionMasked()) {
//            case MotionEvent.ACTION_DOWN:
//                onTouchEvent(ev);
//                initVelocityTracker(ev);
//                break;
//            case MotionEvent.ACTION_MOVE:
//                velocityTrackerCompute(ev);
//
//                if (interceptTouchLastCount != interceptTouchCount) {
//                    interceptTouchLastCount = interceptTouchCount;
//                } else if (Math.abs(movingPointY - actionDownPointY) > Math.abs(movingPointX - actionDownPointX)
//                        || (pullRefreshLayout.moveDistance != 0)) {
//                    if (!isLastMotionPointYSet) {
//                        isLastMotionPointYSet = true;
//                        mLastMotionY = (int) ev.getY();
//                    }
//                    onTouchEvent(ev);
//                }
//                break;
//            case MotionEvent.ACTION_UP:
//            case MotionEvent.ACTION_CANCEL:
//                onTouchEvent(ev);
//                cancelVelocityTracker();
//                velocityY = 0;
//                interceptTouchLastCount = -1;
//                interceptTouchCount = 0;
//                isLastMotionPointYSet = false;
//                resetEvent = false;
//                isMoving = false;
//                break;
//        }
//        return false;
//    }
//
//    boolean onInterceptTouchEvent(MotionEvent ev) {
//        if (pullRefreshLayout.moveDistance != 0) {
//            return true;
//        }
//        switch (ev.getActionMasked()) {
//            case MotionEvent.ACTION_DOWN:
//                actionDownPointX = ev.getX();
//                actionDownPointY = ev.getY();
//                break;
//            case MotionEvent.ACTION_MOVE:
//                interceptTouchCount++;
//                movingPointX = ev.getX();
//                movingPointY = ev.getY();
//                break;
//            case MotionEvent.ACTION_UP:
//            case MotionEvent.ACTION_CANCEL:
//                break;
//        }
//
//        return false;
//    }
//
//    public boolean onTouchEvent(MotionEvent ev) {
//        MotionEvent vtev = MotionEvent.obtain(ev);
//
//        final int actionMasked = MotionEventCompat.getActionMasked(ev);
//
//        if (actionMasked == MotionEvent.ACTION_DOWN) {
//            mNestedYOffset = 0;
//        }
//        vtev.offsetLocation(0, mNestedYOffset);
//
//        switch (actionMasked) {
//            case MotionEvent.ACTION_DOWN: {
//                mLastMotionY = (int) ev.getY();
//                mActivePointerId = ev.getPointerId(0);
//                pullRefreshLayout.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
//                break;
//            }
//            case MotionEvent.ACTION_MOVE:
//                if (mActivePointerId != vtev.getPointerId(0)) {
//                    mLastMotionY = (int) vtev.getY();
//                    mActivePointerId = vtev.getPointerId(0);
//                }
//
//                final int y = (int) ev.getY();
//                int deltaY = mLastMotionY - y;
//
//                if (pullRefreshLayout.dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset)) {
//                    deltaY -= mScrollConsumed[1];
//                    vtev.offsetLocation(0, mScrollOffset[1]);
//                    mNestedYOffset += mScrollOffset[1];
//                }
//                mIsBeingDragged = true;
//                if (mIsBeingDragged) {
//                    mLastMotionY = y - mScrollOffset[1];
//                    final int oldY = pullRefreshLayout.targetView.getScrollY();
//
//                    if (deltaY < 0) {
//                        isDragDown = true;
//                    } else if (deltaY > 0) {
//                        isDragDown = false;
//                    }
//
//                    if ((pullRefreshLayout.moveDistance < 0 && isDragDown)
//                            || (pullRefreshLayout.moveDistance > 0 && !isDragDown)) {
//                        pullRefreshLayout.onNestedPreScroll(null, 0, deltaY, childConsumed);
//                        vtev.offsetLocation(0, childConsumed[1] - lastChildConsumedY);
//                        lastChildConsumedY = childConsumed[1];
//                        return true;
//                    }
//
//                    final int scrolledDeltaY = pullRefreshLayout.targetView.getScrollY() - oldY;
//                    final int unconsumedY = deltaY - scrolledDeltaY;
//
//                    Log.e(TAG, "onTouchEvent: " + scrolledDeltaY + "     " + unconsumedY + "    " + Arrays.toString(mScrollOffset));
//                    if ((isDragDown && !pullRefreshLayout.canChildScrollUp())
//                            || (!isDragDown && !pullRefreshLayout.canChildScrollDown())) {
//                        if (pullRefreshLayout.dispatchNestedScroll(0, scrolledDeltaY, 0, unconsumedY, mScrollOffset)) {
//                            mLastMotionY -= mScrollOffset[1];
//                            vtev.offsetLocation(0, mScrollOffset[1]);
//                            mNestedYOffset += mScrollOffset[1];
//                            pullRefreshLayout.onNestedScroll(null, 0, 0, 0, mScrollOffset[1] == 0 ? deltaY : 0);
//                            return true;
//                        }
//                        pullRefreshLayout.onNestedScroll(null, 0, 0, 0, mScrollOffset[1] == 0 ? deltaY : 0);
//                    }
//                }
//                break;
//            case MotionEvent.ACTION_UP:
//            case MotionEvent.ACTION_CANCEL:
//                if (mIsBeingDragged) {
//                    lastChildConsumedY = 0;
//                    mScrollOffset[1] = 0;
//                    mIsBeingDragged = false;
//                    if (isLastMotionPointYSet) {
//                        if (!pullRefreshLayout.dispatchNestedPreFling(0, -velocityY)) {
//                            pullRefreshLayout.onNestedPreFling(pullRefreshLayout.targetView, 0, -velocityY);
//                        }
//                    }
//                    pullRefreshLayout.onStopNestedScroll(pullRefreshLayout.targetView);
//                }
//                mActivePointerId = -1;
//                break;
//
//        }
//
//        vtev.recycle();
//        return true;
//    }
//
//    /**
//     * velocityTracker dell
//     *
//     * @param ev MotionEvent
//     */
//    private void initVelocityTracker(MotionEvent ev) {
//        if (velocityTracker == null) {
//            velocityTracker = VelocityTracker.obtain();
//        }
//        velocityTracker.addMovement(ev);
//    }
//
//    private void velocityTrackerCompute(MotionEvent ev) {
//        try {
//            velocityTracker.addMovement(ev);
//            velocityTracker.computeCurrentVelocity(1000);
//            velocityY = velocityTracker.getYVelocity();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void cancelVelocityTracker() {
//        try {
//            velocityTracker.clear();
//            velocityTracker.recycle();
//            velocityTracker = null;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    void autoRefreshDell() {
//        movingPointY = 100;
//        actionDownPointY = 0;
//        movingPointX = 0;
//        actionDownPointX = 0;
//    }
//
//}
