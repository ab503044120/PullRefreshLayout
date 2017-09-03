package com.yan.pullrefreshlayout;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by yan on 2017/7/7.
 * refresh show helper
 */
public class ShowGravity {

    /**
     * @ShowState
     */
    @IntDef({FOLLOW, FOLLOW_PLACEHOLDER, FOLLOW_CENTER
            , PLACEHOLDER, PLACEHOLDER_FOLLOW, PLACEHOLDER_CENTER
            , CENTER, CENTER_FOLLOW
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ShowState {
    }

    public static final int FOLLOW = 0;
    public static final int FOLLOW_PLACEHOLDER = 1;
    public static final int FOLLOW_CENTER = 2;
    public static final int PLACEHOLDER = 3;
    public static final int PLACEHOLDER_FOLLOW = 4;
    public static final int PLACEHOLDER_CENTER = 5;
    public static final int CENTER = 6;
    public static final int CENTER_FOLLOW = 7;

    /**
     * show gravity
     * - use by pullRefreshLayout to set show gravity
     */
    int headerShowGravity = FOLLOW;
    int footerShowGravity = FOLLOW;

    private PullRefreshLayout pullRefreshLayout;

    ShowGravity(PullRefreshLayout pullRefreshLayout) {
        this.pullRefreshLayout = pullRefreshLayout;
    }

    void dellHeaderMoving(int moveDistance) {
        if (pullRefreshLayout.headerView != null && moveDistance >= 0) {
            switch (headerShowGravity) {
                case FOLLOW:
                    pullRefreshLayout.headerView.setTranslationY(moveDistance);
                    break;
                case FOLLOW_PLACEHOLDER:
                    pullRefreshLayout.headerView.setTranslationY(moveDistance <= pullRefreshLayout
                            .refreshTriggerDistance ? moveDistance : pullRefreshLayout
                            .refreshTriggerDistance);
                    break;
                case FOLLOW_CENTER:
                    pullRefreshLayout.headerView.setTranslationY(moveDistance <= pullRefreshLayout
                            .refreshTriggerDistance ? moveDistance : pullRefreshLayout.refreshTriggerDistance
                            + (moveDistance - pullRefreshLayout.refreshTriggerDistance) / 2);
                    break;
                case PLACEHOLDER_CENTER:
                    pullRefreshLayout.headerView.setTranslationY(moveDistance <=
                            pullRefreshLayout.refreshTriggerDistance ? 0 : (moveDistance
                            - pullRefreshLayout.refreshTriggerDistance) / 2);
                    break;
                case PLACEHOLDER_FOLLOW:
                    pullRefreshLayout.headerView.setTranslationY(moveDistance <= pullRefreshLayout
                            .refreshTriggerDistance ? 0 : moveDistance
                            - pullRefreshLayout.refreshTriggerDistance);
                    break;
                case CENTER:
                    pullRefreshLayout.headerView.setTranslationY(moveDistance / 2);
                    break;
                case CENTER_FOLLOW:
                    pullRefreshLayout.headerView.setTranslationY(moveDistance <= pullRefreshLayout
                            .refreshTriggerDistance ? moveDistance / 2 : moveDistance
                            - pullRefreshLayout.refreshTriggerDistance / 2);
                    break;
            }
        }
    }

    void dellFooterMoving(int moveDistance) {
        if (pullRefreshLayout.footerView != null && moveDistance <= 0) {
            switch (footerShowGravity) {
                case FOLLOW:
                    pullRefreshLayout.footerView.setTranslationY(moveDistance);
                    break;
                case FOLLOW_PLACEHOLDER:
                    pullRefreshLayout.footerView.setTranslationY(moveDistance >= -pullRefreshLayout
                            .loadTriggerDistance ? moveDistance : -pullRefreshLayout
                            .loadTriggerDistance);
                    break;
                case FOLLOW_CENTER:
                    pullRefreshLayout.footerView.setTranslationY(moveDistance <= -pullRefreshLayout
                            .loadTriggerDistance ? -pullRefreshLayout.loadTriggerDistance
                            + (pullRefreshLayout.loadTriggerDistance + moveDistance) / 2 : moveDistance);
                    break;
                case PLACEHOLDER_CENTER:
                    pullRefreshLayout.footerView.setTranslationY(moveDistance <= -pullRefreshLayout
                            .loadTriggerDistance ? (moveDistance + pullRefreshLayout
                            .loadTriggerDistance) / 2 : 0);
                    break;
                case PLACEHOLDER_FOLLOW:
                    pullRefreshLayout.footerView.setTranslationY(moveDistance <= -pullRefreshLayout
                            .loadTriggerDistance ? moveDistance + pullRefreshLayout
                            .loadTriggerDistance : 0);
                    break;
                case CENTER:
                    pullRefreshLayout.footerView.setTranslationY(moveDistance / 2);
                    break;
                case CENTER_FOLLOW:
                    pullRefreshLayout.footerView.setTranslationY(moveDistance <= -pullRefreshLayout
                            .loadTriggerDistance ? moveDistance + pullRefreshLayout
                            .loadTriggerDistance / 2 : moveDistance / 2);
                    break;
            }
        }
    }

    void layout(int left, int top, int right, int bottom) {
        if (pullRefreshLayout.headerView != null) {
            int paddingLeft = pullRefreshLayout.getPaddingLeft();
            int paddingTop = pullRefreshLayout.getPaddingTop();
            PullRefreshLayout.LayoutParams lp = (PullRefreshLayout.LayoutParams) pullRefreshLayout.headerView.getLayoutParams();
            switch (headerShowGravity) {
                case FOLLOW:
                case FOLLOW_PLACEHOLDER:
                case FOLLOW_CENTER:
                    pullRefreshLayout.headerView.layout(paddingLeft + lp.leftMargin
                            , top + lp.topMargin + paddingTop - pullRefreshLayout.headerView.getMeasuredHeight()
                            , paddingLeft + lp.leftMargin + pullRefreshLayout.headerView.getMeasuredWidth()
                            , top + lp.topMargin + paddingTop);
                    break;
                case PLACEHOLDER:
                case PLACEHOLDER_CENTER:
                case PLACEHOLDER_FOLLOW:
                    pullRefreshLayout.headerView.layout(paddingLeft + lp.leftMargin
                            , top + paddingTop + lp.topMargin
                            , paddingLeft + lp.leftMargin + pullRefreshLayout.headerView.getMeasuredWidth()
                            , top + paddingTop + lp.topMargin + pullRefreshLayout.headerView.getMeasuredHeight());
                    break;
                case CENTER:
                case CENTER_FOLLOW:
                    pullRefreshLayout.headerView.layout(paddingLeft + lp.leftMargin
                            , -pullRefreshLayout.headerView.getMeasuredHeight() / 2
                            , paddingLeft + lp.leftMargin + pullRefreshLayout.headerView.getMeasuredWidth()
                            , pullRefreshLayout.headerView.getMeasuredHeight() / 2);
                    break;
            }
        }
        if (pullRefreshLayout.footerView != null) {
            int paddingLeft = pullRefreshLayout.getPaddingLeft();
            int paddingTop = pullRefreshLayout.getPaddingTop();
            PullRefreshLayout.LayoutParams lp = (PullRefreshLayout.LayoutParams) pullRefreshLayout.footerView.getLayoutParams();
            switch (footerShowGravity) {
                case FOLLOW:
                case FOLLOW_PLACEHOLDER:
                case FOLLOW_CENTER:
                    pullRefreshLayout.footerView.layout(lp.leftMargin + paddingLeft
                            , bottom + lp.topMargin + paddingTop
                            , lp.leftMargin + paddingLeft + pullRefreshLayout.footerView.getMeasuredWidth()
                            , bottom + lp.topMargin + paddingTop + pullRefreshLayout.footerView.getMeasuredHeight());
                    break;
                case PLACEHOLDER:
                case PLACEHOLDER_CENTER:
                case PLACEHOLDER_FOLLOW:
                    pullRefreshLayout.footerView.layout(lp.leftMargin + paddingLeft
                            , bottom + lp.topMargin + paddingTop - pullRefreshLayout.footerView.getMeasuredHeight()
                            , lp.leftMargin + paddingLeft + pullRefreshLayout.footerView.getMeasuredWidth()
                            , bottom + lp.topMargin + paddingTop);
                    break;
                case CENTER:
                case CENTER_FOLLOW:
                    pullRefreshLayout.footerView.layout(lp.leftMargin + paddingLeft
                            , bottom - pullRefreshLayout.footerView.getMeasuredHeight() / 2
                            , lp.leftMargin + paddingLeft + pullRefreshLayout.footerView.getMeasuredWidth()
                            , bottom + pullRefreshLayout.footerView.getMeasuredHeight() / 2);
                    break;
            }
        }
    }
}
