package com.soft.cleargrass.otaupdate;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * Created by dongwei on 2017/4/26.
 */

public class FileBrowserAppsAdapter extends BaseAdapter {

    private final LayoutInflater mInflater;
    private final Resources mResources;

    public FileBrowserAppsAdapter(final Context context) {
        mInflater = LayoutInflater.from(context);
        mResources = context.getResources();
    }

    @Override
    public int getCount() {
        return mResources.getStringArray(R.array.dfu_app_file_browser).length;
    }

    @Override
    public Object getItem(int position) {
        return mResources.getStringArray(R.array.dfu_app_file_browser_action)[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = mInflater.inflate(R.layout.app_file_brower_item, parent, false);
        }

        final TextView item = (TextView) view;
        item.setText(mResources.getStringArray(R.array.dfu_app_file_browser)[position]);
        item.getCompoundDrawablesRelative()[0].setLevel(position);
        return view;
    }
}
