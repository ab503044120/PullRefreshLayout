package com.yan.refreshloadlayouttest.testactivity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nested);
        initData();
        initRefreshLayout();
        initRecyclerView();
        vState = findViewById(R.id.no_data);
        vState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshLayout.autoRefresh();
            }
        });

        refreshLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshLayout.autoRefresh();
            }
        }, 150);
    }

    private void initRecyclerView() {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SimpleAdapter(this, datas);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    private void initRefreshLayout() {
        refreshLayout = (PullRefreshLayout) findViewById(R.id.refreshLayout);
        refreshLayout.setOnRefreshListener(new PullRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                if (vState.getVisibility() == View.VISIBLE) {
                    vState.setVisibility(View.GONE);
                } else {
                    vState.setVisibility(View.VISIBLE);
                }
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
                        datas.add(new SimpleItem(R.drawable.img4, "夏目友人帐"));
                        adapter.notifyItemInserted(datas.size());
                    }
                }, 3000);
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
