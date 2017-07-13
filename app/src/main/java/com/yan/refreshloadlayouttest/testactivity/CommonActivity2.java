package com.yan.refreshloadlayouttest.testactivity;


import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.Toast;

import com.yan.pullrefreshlayout.PullRefreshLayout;
import com.yan.pullrefreshlayout.RefreshShowHelper;
import com.yan.refreshloadlayouttest.HeaderOrFooter;
import com.yan.refreshloadlayouttest.R;

public class CommonActivity2 extends CommonActivity1 {
    protected int getViewId() {
        return R.layout.common_activity2;
    }

    protected void initRefreshLayout() {
        super.initRefreshLayout();
        refreshLayout.setTwinkEnable(true);
        refreshLayout.setHeaderView(new HeaderOrFooter(getBaseContext(), "SemiCircleSpinIndicator"));
        refreshLayout.setFooterView(new HeaderOrFooter(getBaseContext(), "BallScaleRippleMultipleIndicator"));
        refreshLayout.setRefreshShowGravity(RefreshShowHelper.STATE_CENTER, RefreshShowHelper.STATE_CENTER);
        HorizontalScrollView horizontalScrollView = (HorizontalScrollView) findViewById(R.id.hsv);
        final boolean[] isTouch = {false};
        horizontalScrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                isTouch[0] = true;

                if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                    isTouch[0] = false;
                }
                return false;
            }
        });
        refreshLayout.setOnDragIntercept(new PullRefreshLayout.OnDragIntercept() {
            @Override
            public boolean onHeaderDownIntercept() {
                return !isTouch[0];
            }

            @Override
            public boolean onFooterUpIntercept() {
                return !isTouch[0];

            }

        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        refreshLayout.setTwinkEnable(false);
    }
}
