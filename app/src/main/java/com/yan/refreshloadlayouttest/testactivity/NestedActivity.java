package com.yan.refreshloadlayouttest.testactivity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.yan.pullrefreshlayout.PullRefreshLayout;
import com.yan.pullrefreshlayout.PullRefreshView;
import com.yan.pullrefreshlayout.RefreshShowHelper;
import com.yan.refreshloadlayouttest.HeaderOrFooter;
import com.yan.refreshloadlayouttest.R;

import java.util.ArrayList;
import java.util.List;

public class NestedActivity extends AppCompatActivity {
    private static final String TAG = "NestedActivity";
    private List<String> datas;
    private PullRefreshLayout refreshLayout;
    private SimpleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nested);
        initData();
        initRecyclerView();
        initRefreshLayout();
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
//        refreshLayout.setOverScrollDampingRatio(0.4f);
//        refreshLayout.setAdjustTwinkDuring(2);
//        refreshLayout.setTwinkEnable(false);
        refreshLayout.setLoadMoreEnable(true);
//        refreshLayout.setRefreshEnable(false);
//        refreshLayout.setAutoLoadingEnable(true);
//        refreshLayout.setDuringAdjustValue(10f);// 动画执行时间调节，越大动画执行越慢
        // 刷新或加载完成后回复动画执行时间，为-1时，根据setDuringAdjustValue（）方法实现
//        refreshLayout.setRefreshBackTime(300);
//        refreshLayout.setPullViewHeight(400);// 设置头部和底部的高度
//        refreshLayout.setDragDampingRatio(0.6f);// 阻尼系数
        refreshLayout.setRefreshTriggerDistance(dipToPx(getApplicationContext(), 90));
        refreshLayout.setPullLimitDistance(dipToPx(getApplicationContext(), 150));// 拖拽最大范围，为-1时拖拽范围不受限制
//        refreshLayout.setRefreshEnable(false);
        refreshLayout.setHeaderView(new PullRefreshView(getBaseContext()) {
            @Override
            protected int contentView() {
                return R.layout.refresh_view_big;
            }

            @Override
            public void onPullChange(float percent) {
                super.onPullChange(percent);
                if (percent > 1.2) {
                    findViewById(R.id.iv_bg).setScaleY(1 + (percent - 1.2f) *0.2f);
                } else {
                    findViewById(R.id.iv_bg).setScaleY(1f);
                }
            }
        });
        refreshLayout.setFooterView(new HeaderOrFooter(getBaseContext(), "LineScaleIndicator"));
        refreshLayout.setHeaderShowGravity(RefreshShowHelper.STATE_PLACEHOLDER);
//        refreshLayout.setFooterShowGravity(RefreshShowHelper.STATE_PLACEHOLDER);
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
                        datas.add("onLoading测试数据");
                        adapter.notifyItemInserted(datas.size());
                    }
                }, 3000);
            }
        });

    }

    private float dipToPx(Context context, float value) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, metrics);
    }

    protected void initData() {
        datas = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            datas.add("test" + i);
        }
    }

    static class SimpleAdapter extends RecyclerView.Adapter<SimpleAdapter.SimpleViewHolder> {

        /**
         * Item 点击事件监听的回调
         */
        public interface OnItemClickListener {
            void onItemClick(View view, int position);

            void onItemLongClick(View view, int position);
        }

        private OnItemClickListener mOnItemClickListener;

        public void setOnItemClickLitener(OnItemClickListener mOnItemClickListener) {
            this.mOnItemClickListener = mOnItemClickListener;
        }

        private Context context;
        private List<String> datas;

        public SimpleAdapter(Context context, List<String> datas) {
            this.context = context;
            this.datas = datas;
        }

        @Override
        public SimpleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            SimpleViewHolder holder = new SimpleViewHolder(LayoutInflater.from(
                    context).inflate(R.layout.simple_item, parent,
                    false));
            return holder;
        }

        @Override
        public void onBindViewHolder(final SimpleViewHolder holder, int position) {
            holder.tv.setText(datas.get(position));

            if (mOnItemClickListener != null) {
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int pos = holder.getLayoutPosition();
                        mOnItemClickListener.onItemClick(holder.itemView, pos);
                    }
                });

                holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        int pos = holder.getLayoutPosition();
                        mOnItemClickListener.onItemLongClick(holder.itemView, pos);
                        return false;
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return datas.size();
        }

        class SimpleViewHolder extends RecyclerView.ViewHolder {

            TextView tv;

            public SimpleViewHolder(View view) {
                super(view);
                tv = (TextView) view.findViewById(R.id.id_num);
            }
        }
    }
}
