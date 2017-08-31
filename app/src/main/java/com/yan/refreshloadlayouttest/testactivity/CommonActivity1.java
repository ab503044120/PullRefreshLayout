package com.yan.refreshloadlayouttest.testactivity;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.yan.pullrefreshlayout.PullRefreshLayout;
import com.yan.pullrefreshlayout.ShowGravity;
import com.yan.refreshloadlayouttest.HeaderOrFooter;
import com.yan.refreshloadlayouttest.R;

public class CommonActivity1 extends AppCompatActivity {
    private static final String TAG = "CommonActivity1";

    protected PullRefreshLayout refreshLayout;

    protected int getViewId() {
        return R.layout.common_activity1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getViewId());
        initRefreshLayout();
//        ((TextView)findViewById(R.id.tv_data)) .setMovementMethod(ScrollingMovementMethod.getInstance());
        refreshLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshLayout.autoRefresh();
            }
        }, 150);
        if (findViewById(R.id.iv)!=null) {
            Glide.with(this)
                    .load(R.drawable.loading_bg)
                    .into((ImageView) findViewById(R.id.iv));
        }
    }

    protected void initRefreshLayout() {
        refreshLayout = (PullRefreshLayout) findViewById(R.id.refreshLayout);
//        refreshLayout.setOverScrollDampingRatio(0.4f);
//        refreshLayout.setAdjustTwinkDuring(2);
//        refreshLayout.setTwinkEnable(false);
        refreshLayout.setLoadMoreEnable(true);
        refreshLayout.setRefreshShowGravity(ShowGravity.FOLLOW, ShowGravity.FOLLOW);
//        refreshLayout.setRefreshEnable(false);
//        refreshLayout.setAutoLoadingEnable(true);
//        refreshLayout.setDuringAdjustValue(10f);// 动画执行时间调节，越大动画执行越慢
        // 刷新或加载完成后回复动画执行时间，为-1时，根据setDuringAdjustValue（）方法实现
//        refreshLayout.setRefreshBackTime(300);
//        refreshLayout.setPullViewHeight(400);// 设置头部和底部的高度
//        refreshLayout.setDragDampingRatio(0.6f);// 阻尼系数
//        refreshLayout.setPullLimitDistance(400);// 拖拽最大范围，为-1时拖拽范围不受限制
//        refreshLayout.setRefreshEnable(false);
        refreshLayout.setHeaderView(new HeaderOrFooter(getBaseContext(), "LineSpinFadeLoaderIndicator", Color.WHITE, false));

        refreshLayout.setFooterView(new HeaderOrFooter(getBaseContext(), "PacmanIndicator", Color.WHITE, false));

        refreshLayout.setOnRefreshListener(new PullRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.e(TAG, "refreshLayout onRefresh: ");
                refreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshLayout.refreshComplete();
                    }
                }, 3000);
            }

            @Override
            public void onLoading() {
                Log.e(TAG, "refreshLayout onLoading: ");
                refreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshLayout.loadMoreComplete();
                    }
                }, 3000);
            }
        });
    }
}
