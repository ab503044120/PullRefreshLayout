package com.yan.refreshloadlayouttest.testactivity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.yan.pullrefreshlayout.PullRefreshLayout;
import com.yan.refreshloadlayouttest.R;
import com.yan.refreshloadlayouttest.widget.PhoenixHeader;
import com.yan.refreshloadlayouttest.widget.fungame.FunGameBattleCityHeader;
import com.yan.refreshloadlayouttest.widget.fungame.FunGameHitBlockHeader;

import static android.content.ContentValues.TAG;

/**
 * A simple {@link Fragment} subclass.
 */
public class RefreshFragment extends Fragment {
    View root;

    public static RefreshFragment getInstance(int index) {
        RefreshFragment refreshFragment = new RefreshFragment();
        Bundle args = new Bundle();
        args.putInt("index", index);
        refreshFragment.setArguments(args);
        return refreshFragment;
    }

    public RefreshFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (root == null) {
            root = inflater.inflate(R.layout.fragment_refresh, container, false);
            init();
        }
        return root;
    }

    private void init() {
        final PullRefreshLayout refreshLayout = (PullRefreshLayout) root.findViewById(R.id.refreshLayout);
        initHeader(refreshLayout);
        refreshLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshLayout.autoRefresh();
            }
        }, 200);
        refreshLayout.setOnRefreshListener(new PullRefreshLayout.OnRefreshListenerAdapter() {
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
        });
        setImages();
    }

    private void initHeader(PullRefreshLayout refreshLayout) {
        switch (getArguments().getInt("index")) {
            case 1:
                refreshLayout.setHeaderView(new PhoenixHeader(getContext(), refreshLayout));
                break;
            case 2:
                refreshLayout.setHeaderView(new FunGameHitBlockHeader(getContext(), refreshLayout));
                break;
          case 3:
                refreshLayout.setHeaderView(new FunGameBattleCityHeader(getContext(), refreshLayout));
                break;
        }
    }

    private void setImages() {
        Glide.with(this)
                .load(R.drawable.img1)
                .into((ImageView) root.findViewById(R.id.iv1));
        Glide.with(this)
                .load(R.drawable.img2)
                .into((ImageView) root.findViewById(R.id.iv2));
        Glide.with(this)
                .load(R.drawable.img3)
                .into((ImageView) root.findViewById(R.id.iv3));
        Glide.with(this)
                .load(R.drawable.img4)
                .into((ImageView) root.findViewById(R.id.iv4));
        Glide.with(this)
                .load(R.drawable.img5)
                .into((ImageView) root.findViewById(R.id.iv5));
        Glide.with(this)
                .load(R.drawable.img6)
                .into((ImageView) root.findViewById(R.id.iv6));
        Glide.with(this)
                .load(R.drawable.loading_bg)
                .into((ImageView) root.findViewById(R.id.iv7));
    }

}
