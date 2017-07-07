package com.yan.refreshloadlayouttest.testactivity;


import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.yan.pullrefreshlayout.RefreshShowHelper;
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
        refreshLayout.setRefreshShowGravity(RefreshShowHelper.STATE_CENTER,RefreshShowHelper.STATE_CENTER);

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        refreshLayout.setPullTwinkEnable(false);
    }
}
