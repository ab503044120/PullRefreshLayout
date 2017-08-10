package com.yan.refreshloadlayouttest.testactivity;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.bumptech.glide.Glide;
import com.yan.refreshloadlayouttest.R;

import java.util.List;

/**
 * Created by yan on 2017/8/10.
 */

public class SimpleListAdapter extends BaseAdapter {
    private Context context;
    private List<SimpleItem> simpleItems;

    public SimpleListAdapter(Context context, List<SimpleItem> simpleItems) {
        this.context = context;
        this.simpleItems = simpleItems;
    }

    @Override
    public int getCount() {
        return simpleItems.size();
    }


    @Override
    public Object getItem(int position) {
        return simpleItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SimpleViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.simple_item, parent, false);
            convertView.setTag(viewHolder = new SimpleViewHolder(convertView));
        } else {
            viewHolder = (SimpleViewHolder) convertView.getTag();
        }
        viewHolder.tv.setText(simpleItems.get(position).title);
        Glide.with(context)
                .load(simpleItems.get(position).resId)
                .into( viewHolder.iv);

        return convertView;
    }
}
