package com.yan.refreshloadlayouttest.testactivity;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.yan.pullrefreshlayout.PullRefreshLayout;
import com.yan.refreshloadlayouttest.R;

/**
 * 微博列表
 * code modify from SmartRefreshLayout
 */
public class ScrollingActivity2 extends AppCompatActivity {

    private int mOffset = 0;
    private int mScrollY = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling2);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        final View parallax = findViewById(R.id.parallax);
        final View buttonBar = findViewById(R.id.buttonBarLayout);
        final NestedScrollView scrollView = (NestedScrollView) findViewById(R.id.scrollView);
        final PullRefreshLayout refreshLayout = (PullRefreshLayout) findViewById(R.id.refreshLayout);
        refreshLayout.setOnRefreshListener(new PullRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshLayout.refreshComplete();
                    }
                },2000);
            }

            @Override
            public void onLoading() {

            }
        });
        scrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            private int lastScrollY = 0;
            private int h = 340;
            private int color = ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary) & 0x00ffffff;

            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (lastScrollY < h) {
                    scrollY = Math.min(h, scrollY);
                    mScrollY = scrollY > h ? h : scrollY;
                    buttonBar.setAlpha(1f * mScrollY / h);
                    toolbar.setBackgroundColor(((255 * mScrollY / h) << 24) | color);
                    parallax.setTranslationY(mOffset - mScrollY);
                }
                lastScrollY = scrollY;
            }
        });
        buttonBar.setAlpha(0);
        toolbar.setBackgroundColor(0);
        setImages();
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

}
