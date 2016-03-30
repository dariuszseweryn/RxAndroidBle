package com.polidea.rxandroidble.sample.example1_scanning;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.RxBleScanResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

class ScanResultsAdapter extends RecyclerView.Adapter<ScanResultsAdapter.ViewHolder> {

    static class ViewHolder extends RecyclerView.ViewHolder {

        @Bind(android.R.id.text1)
        public TextView line1;
        @Bind(android.R.id.text2)
        public TextView line2;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public interface OnAdapterItemClickListener {

        void onAdapterViewClick(View view);
    }

    private static final Comparator<RxBleScanResult> SORTING_COMPARATOR = (lhs, rhs) -> {
        return lhs.getBleDevice().getMacAddress().compareTo(rhs.getBleDevice().getMacAddress());
    };
    private final List<RxBleScanResult> data = new ArrayList<>();
    private OnAdapterItemClickListener onAdapterItemClickListener;
    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (onAdapterItemClickListener != null) {
                onAdapterItemClickListener.onAdapterViewClick(v);
            }
        }
    };

    public void addScanResult(RxBleScanResult bleScanResult) {
        // Not the best way to ensure distinct devices, just for sake on the demo.

        for (int i = 0; i < data.size(); i++) {

            if (data.get(i).getBleDevice().equals(bleScanResult.getBleDevice())) {
                data.set(i, bleScanResult);
                notifyItemChanged(i);
                return;
            }
        }

        data.add(bleScanResult);
        Collections.sort(data, SORTING_COMPARATOR);
        notifyDataSetChanged();
    }

    public void clearScanResults() {
        data.clear();
        notifyDataSetChanged();
    }

    public RxBleScanResult getItemAtPosition(int childAdapterPosition) {
        return data.get(childAdapterPosition);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final RxBleScanResult rxBleScanResult = data.get(position);
        final RxBleDevice bleDevice = rxBleScanResult.getBleDevice();
        holder.line1.setText(String.format("%s (%s)", bleDevice.getMacAddress(), bleDevice.getName()));
        holder.line2.setText(String.format("RSSI: %d", rxBleScanResult.getRssi()));
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View itemView = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.two_line_list_item, parent, false);
        itemView.setOnClickListener(onClickListener);
        return new ViewHolder(itemView);
    }

    public void setOnAdapterItemClickListener(OnAdapterItemClickListener onAdapterItemClickListener) {
        this.onAdapterItemClickListener = onAdapterItemClickListener;
    }
}
