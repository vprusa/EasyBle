package com.ficat.sample.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.widget.TextView;

import com.ficat.easyble.BleDevice;
import com.ficat.sample.R;

import java.util.List;


public class ScanDeviceAdapter extends CommonRecyclerViewAdapter<BleDevice> {

    public ScanDeviceAdapter(@NonNull Context context, @NonNull List<BleDevice> dataList, @NonNull SparseArray<int[]> resLayoutAndViewIds) {
        super(context, dataList, resLayoutAndViewIds);
    }

    @Override
    public int getItemResLayoutType(int position) {
        return R.layout.item_rv_scan_devices;
    }

    @Override
    public void bindDataToItem(CommonRecyclerViewAdapter.MyViewHolder holder, BleDevice data, int position) {
        TextView name = (TextView) holder.mViews.get(R.id.tv_name);
        TextView address = (TextView) holder.mViews.get(R.id.tv_address);
        TextView connectionState = (TextView) holder.mViews.get(R.id.tv_connection_state);
        name.setText(data.name);
        address.setText(data.address);
        if (data.connected) {
            connectionState.setTextColor(mContext.getResources().getColor(R.color.bright_blue));
            connectionState.setText("connected");
        } else {
            connectionState.setTextColor(mContext.getResources().getColor(R.color.bright_red));
            connectionState.setText("disconnected");
        }
    }
}
