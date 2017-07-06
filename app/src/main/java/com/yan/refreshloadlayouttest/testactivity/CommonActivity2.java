package com.yan.refreshloadlayouttest.testactivity;


import android.os.Bundle;

import com.yan.refreshloadlayouttest.HeaderOrFooter;
import com.yan.refreshloadlayouttest.R;

public class CommonActivity2 extends CommonActivity1 {
    protected int getViewId() {
        return R.layout.common_activity2;
    }

    protected void initRefreshLayout() {
        super.initRefreshLayout();
        refreshLayout.setHeaderView(new HeaderOrFooter(getBaseContext(), "SemiCircleSpinIndicator"));
        refreshLayout.setFooterView(new HeaderOrFooter(getBaseContext(), "BallScaleRippleMultipleIndicator"));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        refreshLayout.setPullTwinkEnable(false);
    }
}
