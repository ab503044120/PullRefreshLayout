package com.yan.refreshloadlayouttest.testactivity;


import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.bumptech.glide.Glide;
import com.yan.pullrefreshlayout.PullRefreshLayout;
import com.yan.pullrefreshlayout.ShowGravity;
import com.yan.refreshloadlayouttest.HeaderOrFooter;
import com.yan.refreshloadlayouttest.R;
import com.yan.refreshloadlayouttest.widget.ClassicLoadView;

public class CommonActivity2 extends CommonActivity1 {
    protected int getViewId() {
        return R.layout.common_activity2;
    }

    ClassicLoadView classicLoadView;

    ScrollView scrollView;

    LinearLayout linearLayout;

    protected void initRefreshLayout() {
        refreshLayout = (PullRefreshLayout) findViewById(R.id.refreshLayout);
        scrollView = (ScrollView) findViewById(R.id.sv);
        linearLayout = (LinearLayout) findViewById(R.id.ll);
        setImages();
        refreshLayout.setTwinkEnable(true);
        refreshLayout.setAutoLoadingEnable(true);
        refreshLayout.setLoadMoreEnable(true);
        refreshLayout.setHeaderView(new HeaderOrFooter(getBaseContext(), "SemiCircleSpinIndicator"));
        refreshLayout.setFooterView(classicLoadView = new ClassicLoadView(getApplicationContext(), refreshLayout));
        refreshLayout.setLoadTriggerDistance((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, getResources().getDisplayMetrics()));
        refreshLayout.setTargetView(scrollView);

        HorizontalScrollView horizontalScrollView = (HorizontalScrollView) findViewById(R.id.hsv);
        final boolean[] isTouch = {false};
        horizontalScrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                isTouch[0] = event.getActionMasked() == MotionEvent.ACTION_MOVE;
                return false;
            }
        });
        refreshLayout.setOnDragIntercept(new PullRefreshLayout.OnDragIntercept() {
            @Override
            public boolean onHeaderDownIntercept() {
                return !isTouch[0];
            }

            @Override
            public boolean onFooterUpIntercept() {
                return !isTouch[0];

            }

        });

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
                if (!refreshLayout.isTwinkEnable()) {
                    refreshLayout.autoLoading();
                }
                refreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        linearLayout.addView(LayoutInflater.from(getApplicationContext()).inflate(R.layout.simple_item, null));
                        refreshLayout.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                scrollView.scrollBy(0, -refreshLayout.getMoveDistance());
                                classicLoadView.startBackAnimation();
                            }
                        }, 150);
                    }
                }, 2000);
            }
        });
    }

    private void setImages() {
        Glide.with(this)
                .load(R.drawable.img1)
                .into((ImageView) findViewById(R.id.iv1));
        Glide.with(this)
                .load(R.drawable.img2)
                .into((ImageView) findViewById(R.id.iv2));
        Glide.with(this)
                .load(R.drawable.img3)
                .into((ImageView) findViewById(R.id.iv3));
        Glide.with(this)
                .load(R.drawable.img4)
                .into((ImageView) findViewById(R.id.iv4));
        Glide.with(this)
                .load(R.drawable.img5)
                .into((ImageView) findViewById(R.id.iv5));
        Glide.with(this)
                .load(R.drawable.img6)
                .into((ImageView) findViewById(R.id.iv6));
        Glide.with(this)
                .load(R.drawable.loading_bg)
                .into((ImageView) findViewById(R.id.iv7));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        refreshLayout.setTwinkEnable(false);
    }
}
