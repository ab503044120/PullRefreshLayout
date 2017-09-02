package com.yan.refreshloadlayouttest.testactivity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;

import com.yan.refreshloadlayouttest.widget.ClassicLoadView;
import com.yan.pullrefreshlayout.PullRefreshLayout;
import com.yan.refreshloadlayouttest.R;

import java.util.ArrayList;
import java.util.List;

public class NestedActivity extends AppCompatActivity {
    private static final String TAG = "NestedActivity";
    private List<SimpleItem> datas;
    private PullRefreshLayout refreshLayout;
    private SimpleAdapter adapter;
    private View vState;
    private RecyclerView recyclerView;

    private ClassicLoadView classicLoadView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nested);
        initData();
        initRefreshLayout();
        initRecyclerView();
        vState = findViewById(R.id.no_data);

        refreshLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshLayout.autoRefresh();
            }
        }, 150);
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
        refreshLayout.setFooterView(classicLoadView = new ClassicLoadView(getApplicationContext(), refreshLayout));
        refreshLayout.setLoadTriggerDistance((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, getResources().getDisplayMetrics()));
        refreshLayout.setOnRefreshListener(new PullRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (vState.getVisibility() == View.VISIBLE) {
                            vState.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                            refreshLayout.setAutoLoadingEnable(true);
                            refreshLayout.setLoadMoreEnable(true);
                            refreshLayout.setTargetView(recyclerView);
                            refreshLayout.setFooterView(classicLoadView);
                        } else {
                            refreshLayout.setAutoLoadingEnable(false);
                            refreshLayout.setLoadMoreEnable(false);
                            refreshLayout.setTargetView(vState);
                            vState.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                            refreshLayout.setFooterView(null);
                        }
                        refreshLayout.refreshComplete();
                    }
                }, 3000);
            }

            @Override
            public void onLoading() {
                refreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (datas.size() > 10) {
                            classicLoadView.loadFinish();
                            return;
                        }
                        datas.add(new SimpleItem(R.drawable.img4, "夏目友人帐"));
                        adapter.notifyItemInserted(datas.size());
                        refreshLayout.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // 阻止refreshLayout的默认事件分发
                                recyclerView.scrollBy(0, -refreshLayout.getMoveDistance());
                                classicLoadView.startBackAnimation();
                            }
                        }, 250);
                    }
                }, 1000);
            }
        });
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
