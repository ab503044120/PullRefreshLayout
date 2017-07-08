package com.yan.refreshloadlayouttest.testactivity;


import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.yan.pullrefreshlayout.PullRefreshLayout;
import com.yan.pullrefreshlayout.RefreshShowHelper;
import com.yan.refreshloadlayouttest.HeaderOrFooter;
import com.yan.refreshloadlayouttest.R;

public class CommonActivity3 extends Activity {
    private static final String TAG = "CommonActivity3";

    protected PullRefreshLayout refreshLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.common_activity3);
        initListView();
        initRefreshLayout();
        refreshLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshLayout.autoRefresh();
            }
        }, 150);
    }

    private void initListView() {
        ListView listView = (ListView) findViewById(R.id.lv_data);
        listView.setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_list_item_1, android.R.id.text1, new String[]{
                "test"
                , "test"
                , "test"
                , "test"
                , "test"
                , "test"
                , "test"
                , "test"
                , "test"
                , "test"
                , "test"
                , "test"
                , "test"
                , "test"
                , "test"
                , "test"
                , "test"
                , "test"
                , "test"
                , "test"
                , "test"
                , "test"
                , "test"
                , "test"
                , "test"
                , "test"
                , "test"
                , "test"
                , "test"
        }));
    }

    private void initRefreshLayout() {
        refreshLayout = (PullRefreshLayout) findViewById(R.id.refreshLayout);
        refreshLayout.setOverScrollDampingRatio(0.15f);
//        refreshLayout.setAdjustTwinkDuring(2);
//        refreshLayout.setPullTwinkEnable(false);
        refreshLayout.setLoadMoreEnable(true);
//        refreshLayout.setRefreshEnable(false);
//        refreshLayout.setAutoLoadingEnable(true);
//        refreshLayout.setDuringAdjustValue(10f);// 动画执行时间调节，越大动画执行越慢
        // 刷新或加载完成后回复动画执行时间，为-1时，根据setDuringAdjustValue（）方法实现
//        refreshLayout.setRefreshBackTime(300);
//        refreshLayout.setPullViewHeight(400);// 设置头部和底部的高度
//        refreshLayout.setDragDampingRatio(0.6f);// 阻尼系数
//        refreshLayout.setPullLimitDistance(400);// 拖拽最大范围，为-1时拖拽范围不受限制
//        refreshLayout.setRefreshEnable(false);
        refreshLayout.setHeaderView(new HeaderOrFooter(getBaseContext(), "LineSpinFadeLoaderIndicator"));
        refreshLayout.setFooterView(new HeaderOrFooter(getBaseContext(), "LineSpinFadeLoaderIndicator"));
        refreshLayout.setRefreshShowGravity(RefreshShowHelper.STATE_PLACEHOLDER_CENTER, RefreshShowHelper.STATE_PLACEHOLDER_CENTER);
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
