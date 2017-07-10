package com.yan.refreshloadlayouttest.testactivity;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.yan.pullrefreshlayout.PullRefreshLayout;
import com.yan.refreshloadlayouttest.HeaderOrFooter;
import com.yan.refreshloadlayouttest.R;

public class ScrollingActivity2 extends AppCompatActivity {
    private static final String TAG = "NestedActivity";
    private PullRefreshLayout refreshLayout;
    AppBarLayout appBarLayout;
    private int verticalOffset;
    private NestedScrollView nestedScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling2);
        appBarLayout = (AppBarLayout) findViewById(R.id.app_bar);
        nestedScrollView = (NestedScrollView) findViewById(R.id.nsv_scroll);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        initRefreshLayout();
        refreshLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshLayout.autoRefresh();
            }
        }, 150);
    }

    private void initRefreshLayout() {
        refreshLayout = (PullRefreshLayout) findViewById(R.id.refreshLayout);
        refreshLayout.setPullTwinkEnable(true);
        refreshLayout.setLoadMoreEnable(true);
        refreshLayout.setTargetView(nestedScrollView);
        refreshLayout.setHeaderView(new HeaderOrFooter(getBaseContext(), "BallClipRotatePulseIndicator", Color.WHITE));
        refreshLayout.setFooterView(new HeaderOrFooter(getBaseContext(), "LineScaleIndicator", Color.WHITE));
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
                refreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshLayout.loadMoreComplete();
                    }
                }, 3000);
            }
        });
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                ScrollingActivity2.this.verticalOffset = verticalOffset;
            }
        });
        refreshLayout.setOnPullAbleCheck(
                new PullRefreshLayout.OnPullAbleCheck() {
                    @Override
                    public boolean onCheckPullDownAble() {
                        Log.e(TAG, "onCheckPullDownAble: "+verticalOffset);
                        return verticalOffset >= 0;
                    }
                }
        );
    }
}
