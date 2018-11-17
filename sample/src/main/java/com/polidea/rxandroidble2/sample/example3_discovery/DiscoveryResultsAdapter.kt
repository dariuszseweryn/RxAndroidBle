package com.polidea.rxandroidble2.sample.example3_discovery

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.polidea.rxandroidble2.RxBleDeviceServices
import com.polidea.rxandroidble2.sample.R
import java.util.UUID

internal class DiscoveryResultsAdapter : RecyclerView.Adapter<DiscoveryResultsAdapter.ViewHolder>() {

    private val data = mutableListOf<AdapterItem>()

    var onAdapterItemClickListener: View.OnClickListener? = null

    class AdapterItem(val type: Int, val description: String, val uuid: UUID) {
        companion object {
            const val SERVICE = 1
            const val CHARACTERISTIC = 2
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @BindView(android.R.id.text1)
        lateinit var line1: TextView

        @BindView(android.R.id.text2)
        lateinit var line2: TextView

        init {
            ButterKnife.bind(this, itemView)
        }
    }

    override fun getItemCount(): Int = data.size

    override fun getItemViewType(position: Int): Int = getItem(position).type

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position).run {
            when (holder.itemViewType) {
                AdapterItem.SERVICE -> holder.line1.text = String.format("Service: %s", description)
                AdapterItem.CHARACTERISTIC -> holder.line1.text = String.format("Characteristic: %s", description)
            }
            holder.line2.text = uuid.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = when (viewType) {
            AdapterItem.SERVICE -> R.layout.item_discovery_service
            AdapterItem.CHARACTERISTIC -> R.layout.item_discovery_characteristic
            else -> 0 // should not occur
        }
        return LayoutInflater.from(parent.context)
            .inflate(layout, parent, false)
            .run {
                setOnClickListener(onAdapterItemClickListener)
                ViewHolder(this)
            }
    }

    fun swapScanResult(services: RxBleDeviceServices) {
        with(data) {
            clear()
            services.bluetoothGattServices.forEach {
                // Add service
                val serviceItem = AdapterItem(AdapterItem.SERVICE, it.serviceType, it.uuid)
                add(serviceItem)
                it.characteristics.forEach { characteristic ->
                    val characteristicItem =
                        AdapterItem(
                            AdapterItem.CHARACTERISTIC,
                            characteristic.describeProperties(),
                            characteristic.uuid
                        )
                    add(characteristicItem)
                }
            }
        }
        notifyDataSetChanged()
    }

    fun getItem(position: Int): AdapterItem = data[position]
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