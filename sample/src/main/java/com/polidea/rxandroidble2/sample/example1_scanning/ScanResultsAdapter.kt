package com.polidea.rxandroidble2.sample.example1_scanning

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.polidea.rxandroidble2.scan.ScanResult

internal class ScanResultsAdapter : RecyclerView.Adapter<ScanResultsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @BindView(android.R.id.text1)
        lateinit var line1: TextView

        @BindView(android.R.id.text2)
        lateinit var line2: TextView

        init {
            ButterKnife.bind(this, itemView)
        }
    }

    private val data = mutableListOf<ScanResult>()

    var onAdapterItemClickListener: View.OnClickListener? = null

    fun addScanResult(bleScanResult: ScanResult) {
        // Not the best way to ensure distinct devices, just for the sake of the demo.
        data.withIndex()
            .firstOrNull { it.value.bleDevice == bleScanResult.bleDevice }
            ?.let { data[it.index] = bleScanResult }
            ?: run {
                with(data) {
                    add(bleScanResult)
                    sortBy { it.bleDevice.macAddress }
                }
                notifyDataSetChanged()
            }
    }

    fun clearScanResults() {
        data.clear()
        notifyDataSetChanged()
    }

    fun itemAtPosition(childAdapterPosition: Int): ScanResult = data[childAdapterPosition]

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(data[position]) {
            holder.line1.text = String.format("%s (%s)", bleDevice.macAddress, bleDevice.name)
            holder.line2.text = String.format("RSSI: %d", rssi)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        LayoutInflater.from(parent.context)
            .inflate(android.R.layout.two_line_list_item, parent, false)
            .run {
                setOnClickListener(onAdapterItemClickListener)
                ViewHolder(this)
            }
}
