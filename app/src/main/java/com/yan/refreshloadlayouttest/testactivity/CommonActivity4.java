package com.yan.refreshloadlayouttest.testactivity;


import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.yan.pullrefreshlayout.PullRefreshLayout;
import com.yan.refreshloadlayouttest.R;

import java.util.ArrayList;
import java.util.List;

public class CommonActivity4 extends Activity {
    private List<String> datas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.common_activity4);
        initData();
        initListView();
        initRecyclerView();
        initRefreshLayout();
    }

    private void initListView() {
        ListView listView = (ListView) findViewById(R.id.lv_data);
        listView.setAdapter(new ArrayAdapter<>(getBaseContext(), android.R.layout.simple_list_item_1, android.R.id.text1
                , datas.toArray(new String[datas.size()])));
    }

    private void initRefreshLayout() {
        ((PullRefreshLayout) findViewById(R.id.refreshLayout1)).setRefreshEnable(false);
        ((PullRefreshLayout) findViewById(R.id.refreshLayout2)).setRefreshEnable(false);
        ((PullRefreshLayout) findViewById(R.id.refreshLayout1)).setOverScrollDampingRatio(0.08f);
        ((PullRefreshLayout) findViewById(R.id.refreshLayout2)).setOverScrollDampingRatio(0.1f);
    }

    private void initRecyclerView() {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.rv_data);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        NestedActivity.SimpleAdapter adapter = new NestedActivity.SimpleAdapter(this, datas);

        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    protected void initData() {
        datas = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            datas.add("test" + i);
        }
    }
}