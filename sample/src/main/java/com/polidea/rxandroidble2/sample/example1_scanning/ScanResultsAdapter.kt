package com.polidea.rxandroidble2.sample.example1_scanning

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.polidea.rxandroidble2.RxBleDevice

import com.polidea.rxandroidble2.scan.ScanResult
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator

import butterknife.BindView
import butterknife.ButterKnife
import java.util.Locale

internal class ScanResultsAdapter : RecyclerView.Adapter<ScanResultsAdapter.ViewHolder>() {
    private val data = ArrayList<ScanResult>()
    private var onAdapterItemClickListener: OnAdapterItemClickListener? = null
    private val onClickListener = View.OnClickListener { v ->
        if (onAdapterItemClickListener != null) {
            onAdapterItemClickListener!!.onAdapterViewClick(v)
        }
    }

    internal class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @BindView(android.R.id.text1)
        var line1: TextView? = null
        @BindView(android.R.id.text2)
        var line2: TextView? = null

        init {
            ButterKnife.bind(this, itemView)
        }
    }

    internal interface OnAdapterItemClickListener {

        fun onAdapterViewClick(view: View)
    }

    fun addScanResult(bleScanResult: ScanResult) {
        // Not the best way to ensure distinct devices, just for sake on the demo.

        for (i in data.indices) {

            if (data[i].bleDevice == bleScanResult.bleDevice) {
                data[i] = bleScanResult
                notifyItemChanged(i)
                return
            }
        }

        data.add(bleScanResult)
        Collections.sort(data, SORTING_COMPARATOR)
        notifyDataSetChanged()
    }

    fun clearScanResults() {
        data.clear()
        notifyDataSetChanged()
    }

    fun getItemAtPosition(childAdapterPosition: Int): ScanResult {
        return data[childAdapterPosition]
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val rxBleScanResult = data[position]
        val bleDevice = rxBleScanResult.bleDevice
        holder.line1!!.text = String.format(Locale.getDefault(), "%s (%s)", bleDevice.macAddress, bleDevice.name)
        holder.line2!!.text = String.format(Locale.getDefault(), "RSSI: %d", rxBleScanResult.rssi)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(android.R.layout.two_line_list_item, parent, false)
        itemView.setOnClickListener(onClickListener)
        return ViewHolder(itemView)
    }

    fun setOnAdapterItemClickListener(onAdapterItemClickListener: OnAdapterItemClickListener) {
        this.onAdapterItemClickListener = onAdapterItemClickListener
    }

    companion object {

        private val SORTING_COMPARATOR =
            { lhs, rhs -> lhs.getBleDevice().getMacAddress().compareTo(rhs.getBleDevice().getMacAddress()) }
    }
}
