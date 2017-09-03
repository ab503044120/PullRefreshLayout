package com.yan.refreshloadlayouttest.testactivity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.yan.pullrefreshlayout.PullRefreshLayout;
import com.yan.pullrefreshlayout.ShowGravity;
import com.yan.refreshloadlayouttest.R;
import com.yan.refreshloadlayouttest.widget.MaterialHeader;

import java.util.ArrayList;
import java.util.List;

public class NestedActivity2 extends AppCompatActivity {
    private static final String TAG = "NestedActivity";
    private List<SimpleItem> datas;
    private PullRefreshLayout refreshLayout;
    private SimpleAdapter adapter;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nested2);
        initData();
        initRefreshLayout();
        initRecyclerView();
    }

    private void initRecyclerView() {
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SimpleAdapter(this, datas);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    private void initRefreshLayout() {
        refreshLayout = (PullRefreshLayout) findViewById(R.id.refreshLayout);
        refreshLayout.setTwinkEnable(true);
        refreshLayout.setAutoLoadingEnable(false);

        refreshLayout.setRefreshTriggerDistance(300);
        refreshLayout.setLoadTriggerDistance(300);
        refreshLayout.setPullDownLimitDistance(500);
        refreshLayout.setPullUpLimitDistance(500);
        refreshLayout.setHeaderView(new MaterialHeader(getBaseContext(),refreshLayout, 500F / 300));// 触发距离/拖动范围
        refreshLayout.setHeaderShowGravity(ShowGravity.FOLLOW);
        refreshLayout.setHeaderFront(true);
        refreshLayout.setFooterView(new MaterialHeader(getBaseContext(),refreshLayout, 500F/ 300));
        refreshLayout.setFooterShowGravity(ShowGravity.FOLLOW);
        refreshLayout.setFooterFront(true);
        refreshLayout.setMoveWithContent(false);

        refreshLayout.setOnRefreshListener(new PullRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
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

                        datas.add(new SimpleItem(R.drawable.img4, "夏目友人帐"));
                        adapter.notifyItemInserted(datas.size());
                        refreshLayout.loadMoreComplete();
                    }
                }, 1000);
            }
        });

        refreshLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshLayout.autoRefresh();
            }
        }, 150);
    }

    protected void initData() {
        datas = new ArrayList<>();

        datas.add(new SimpleItem(R.drawable.img1, "夏目友人帐"));
        datas.add(new SimpleItem(R.drawable.img2, "夏目友人帐"));
        datas.add(new SimpleItem(R.drawable.img3, "夏目友人帐"));
        datas.add(new SimpleItem(R.drawable.img4, "夏目友人帐"));
        datas.add(new SimpleItem(R.drawable.img5, "夏目友人帐"));
        datas.add(new SimpleItem(R.drawable.img6, "夏目友人帐"));

    }

}
