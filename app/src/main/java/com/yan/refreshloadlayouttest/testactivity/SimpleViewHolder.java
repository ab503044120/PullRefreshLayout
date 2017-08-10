package com.yan.refreshloadlayouttest.testactivity;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.yan.refreshloadlayouttest.R;

/**
 * Created by yan on 2017/8/10.
 */

public class SimpleViewHolder extends RecyclerView.ViewHolder {

    TextView tv;
    ImageView iv;

    public SimpleViewHolder(View view) {
        super(view);
        tv = (TextView) view.findViewById(R.id.id_num);
        iv = (ImageView) view.findViewById(R.id.iv);
    }
}