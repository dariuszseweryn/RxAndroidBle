package com.polidea.rxandroidble2.samplekotlin.example3_discovery

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.polidea.rxandroidble2.RxBleDeviceServices
import com.polidea.rxandroidble2.samplekotlin.R
import com.polidea.rxandroidble2.samplekotlin.example3_discovery.DiscoveryResultsAdapter.ViewHolder
import java.util.UUID

internal class DiscoveryResultsAdapter(
    private val onClickListener: (AdapterItem) -> Unit
) : RecyclerView.Adapter<ViewHolder>() {

    class AdapterItem(val type: Int, val description: String, val uuid: UUID) {
        companion object {
            const val SERVICE = 1
            const val CHARACTERISTIC = 2
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val line1: TextView = itemView.findViewById(android.R.id.text1)

        val line2: TextView = itemView.findViewById(android.R.id.text2)
    }

    private var data = listOf<AdapterItem>()

    override fun getItemCount(): Int = data.size

    override fun getItemViewType(position: Int): Int = data[position].type

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        data[position].run {
            when (holder.itemViewType) {
                AdapterItem.SERVICE -> holder.line1.text = String.format("Service: %s", description)
                AdapterItem.CHARACTERISTIC -> holder.line1.text = String.format("Characteristic: %s", description)
            }
            holder.line2.text = uuid.toString()
        }
        holder.itemView.setOnClickListener { onClickListener(data[position]) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        @LayoutRes val layout = when (viewType) {
            AdapterItem.SERVICE -> R.layout.item_discovery_service
            AdapterItem.CHARACTERISTIC -> R.layout.item_discovery_characteristic
            else -> 0 // should not occur
        }
        return LayoutInflater.from(parent.context)
            .inflate(layout, parent, false)
            .let { ViewHolder(it) }
    }

    fun swapScanResult(services: RxBleDeviceServices) {
        data = services.bluetoothGattServices.flatMap { service ->
            // Add service
            val adapterItems = mutableListOf(AdapterItem(AdapterItem.SERVICE, service.serviceType, service.uuid))
            // Add all characteristics of current service as subsequent items
            service.characteristics.map { characteristic ->
                AdapterItem(AdapterItem.CHARACTERISTIC, characteristic.describeProperties(), characteristic.uuid)
            }.let { characteristicList ->
                adapterItems.addAll(characteristicList)
            }
            adapterItems
        }
        notifyDataSetChanged()
    }
}

private val BluetoothGattService.serviceType: String
    get() = when (type) {
        BluetoothGattService.SERVICE_TYPE_PRIMARY -> "primary"
        else -> "secondary"
    }

private fun BluetoothGattCharacteristic.describeProperties(): String =
    mutableListOf<String>().run {
        if (isReadable) add("Read")
        if (isWriteable) add("Write")
        if (isNotifiable) add("Notify")
        joinToString(" ")
    }

private val BluetoothGattCharacteristic.isNotifiable: Boolean
    get() = properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0

private val BluetoothGattCharacteristic.isReadable: Boolean
    get() = properties and BluetoothGattCharacteristic.PROPERTY_READ != 0

private val BluetoothGattCharacteristic.isWriteable: Boolean
    get() = properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0