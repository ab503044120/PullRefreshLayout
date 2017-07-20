package com.yan.pullrefreshlayout;

import android.support.annotation.IntDef;
import android.view.Gravity;
import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by yan on 2017/7/7.
 * refresh show helper
 */

public class RefreshShowHelper {

    /**
     * @ShowState
     */
    @IntDef({STATE_FOLLOW, STATE_PLACEHOLDER_FOLLOW
            , STATE_PLACEHOLDER_CENTER, STATE_CENTER
            , STATE_CENTER_FOLLOW, STATE_FOLLOW_CENTER
            , STATE_PLACEHOLDER
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ShowState {
    }

    public static final int STATE_FOLLOW = 0;
    public static final int STATE_PLACEHOLDER_FOLLOW = 1;
    public static final int STATE_PLACEHOLDER_CENTER = 2;
    public static final int STATE_CENTER_FOLLOW = 3;
    public static final int STATE_FOLLOW_CENTER = 4;
    public static final int STATE_PLACEHOLDER = 5;
    public static final int STATE_CENTER = Gravity.CENTER_VERTICAL;

    private int headerShowState = STATE_FOLLOW;
    private int footerShowState = STATE_FOLLOW;

    private PullRefreshLayout pullRefreshLayout;

    RefreshShowHelper(PullRefreshLayout pullRefreshLayout) {
        this.pullRefreshLayout = pullRefreshLayout;
    }

    float headerOffsetRatio(float ratio) {
        switch (headerShowState) {
            case STATE_PLACEHOLDER_FOLLOW:
                break;
            case STATE_PLACEHOLDER_CENTER:
                break;
            case STATE_CENTER_FOLLOW:
                break;
            case STATE_FOLLOW_CENTER:
                break;
        }
        return ratio;
    }

    float footerOffsetRatio(float ratio) {
        switch (footerShowState) {
            case STATE_PLACEHOLDER_FOLLOW:
                break;
            case STATE_PLACEHOLDER_CENTER:
                break;
            case STATE_CENTER_FOLLOW:
                break;
            case STATE_FOLLOW_CENTER:
                break;
        }
        return ratio;
    }

    void setHeaderShowGravity(int headerShowGravity) {
        this.headerShowState = headerShowGravity;
    }

    void setFooterShowGravity(int footerShowGravity) {
        this.footerShowState = footerShowGravity;
    }

    void dellHeaderFooterMoving(int moveDistance) {
        if (pullRefreshLayout.headerView != null) {
            switch (headerShowState) {
                case STATE_FOLLOW:
                    pullRefreshLayout.headerView.setTranslationY(moveDistance);
                    break;
            }
        }
        if (pullRefreshLayout.footerView != null) {
            switch (headerShowState) {
                case STATE_FOLLOW:
                    pullRefreshLayout.footerView.setTranslationY(moveDistance);
                    break;
            }
        }
    }

    void layout(int left, int top, int right, int bottom) {
        if (pullRefreshLayout.headerView != null) {
            switch (headerShowState) {
                case STATE_FOLLOW:
                    pullRefreshLayout.headerView.layout(left, top - pullRefreshLayout.headerView.getMeasuredHeight(), right, 0);
                    break;
                case STATE_PLACEHOLDER:
                    pullRefreshLayout.headerView.layout(left, top, right, top + pullRefreshLayout.headerView.getMeasuredHeight());
                    break;
            }
        }
        if (pullRefreshLayout.footerView != null) {
            switch (headerShowState) {
                case STATE_FOLLOW:
                    pullRefreshLayout.footerView.layout(left, bottom, right, bottom + pullRefreshLayout.footerView.getMeasuredHeight());
                    break;
                case STATE_PLACEHOLDER:
                    pullRefreshLayout.footerView.layout(left, bottom - pullRefreshLayout.footerView.getMeasuredHeight(), right, bottom);
                    break;
            }
        }
    }
}
