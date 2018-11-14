package com.polidea.rxandroidble2.sample.example3_discovery

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.polidea.rxandroidble2.RxBleDeviceServices
import com.polidea.rxandroidble2.sample.R
import java.util.ArrayList
import java.util.UUID

internal class DiscoveryResultsAdapter : RecyclerView.Adapter<DiscoveryResultsAdapter.ViewHolder>() {

    private val data = ArrayList<AdapterItem>()
    private var onAdapterItemClickListener: OnAdapterItemClickListener? = null
    private val onClickListener = View.OnClickListener { v ->
        onAdapterItemClickListener?.invoke(v)
    }

    internal class AdapterItem(val type: Int, val description: String, val uuid: UUID) {
        companion object {

            val SERVICE = 1
            val CHARACTERISTIC = 2
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

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemViewType = holder.itemViewType
        val item = getItem(position)

        if (itemViewType == AdapterItem.SERVICE) {
            holder.line1!!.text = String.format("Service: %s", item.description)
        } else {
            holder.line1!!.text = String.format("Characteristic: %s", item.description)
        }

        holder.line2!!.text = item.uuid.toString()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout =
            if (viewType == AdapterItem.SERVICE) R.layout.item_discovery_service else R.layout.item_discovery_characteristic
        val itemView = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        itemView.setOnClickListener(onClickListener)
        return ViewHolder(itemView)
    }

    fun setOnAdapterItemClickListener(onAdapterItemClickListener: OnAdapterItemClickListener) {
        this.onAdapterItemClickListener = onAdapterItemClickListener
    }

    fun swapScanResult(services: RxBleDeviceServices) {
        data.clear()

        for (service in services.bluetoothGattServices) {
            // Add service
            data.add(AdapterItem(AdapterItem.SERVICE, getServiceType(service), service.uuid))
            val characteristics = service.characteristics

            for (characteristic in characteristics) {
                data.add(
                    AdapterItem(
                        AdapterItem.CHARACTERISTIC,
                        describeProperties(characteristic),
                        characteristic.uuid
                    )
                )
            }
        }

        notifyDataSetChanged()
    }

    private fun describeProperties(characteristic: BluetoothGattCharacteristic): String {
        val properties = ArrayList<String>()
        if (isCharacteristicReadable(characteristic)) properties.add("Read")
        if (isCharacteristicWriteable(characteristic)) properties.add("Write")
        if (isCharacteristicNotifiable(characteristic)) properties.add("Notify")
        return TextUtils.join(" ", properties)
    }

    fun getItem(position: Int): AdapterItem {
        return data[position]
    }

    private fun getServiceType(service: BluetoothGattService): String {
        return if (service.type == BluetoothGattService.SERVICE_TYPE_PRIMARY) "primary" else "secondary"
    }

    private fun isCharacteristicNotifiable(characteristic: BluetoothGattCharacteristic): Boolean {
        return characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
    }

    private fun isCharacteristicReadable(characteristic: BluetoothGattCharacteristic): Boolean {
        return characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0
    }

    private fun isCharacteristicWriteable(characteristic: BluetoothGattCharacteristic): Boolean {
        return characteristic.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
    }
}

internal typealias OnAdapterItemClickListener = (View) -> Unit