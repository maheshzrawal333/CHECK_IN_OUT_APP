package com.maheshz.checkinout.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import java.nio.ByteBuffer
import java.util.UUID

class FpPacketAdvertiser(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    // UUID exactly matches the Kiosk's LUMEN_SERVICE_UUID
    private val BLE_KIOSK_UUID = UUID.fromString("0000FEAA-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    fun startAdvertising(employeeCode: String, timestamp: Long, signature: ByteArray, onComplete: () -> Unit) {
        val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser ?: return

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .setTimeout(10000) // Broadcast for exactly 10 seconds
            .build()

        // Byte alignment perfectly matches the Kiosk parser
        // Kiosk expects: 8 Bytes (Emp Code) + 8 Bytes (Timestamp Long) + N Bytes (Signature)
        val empCodeBytes = employeeCode.padEnd(8, '\u0000').toByteArray(Charsets.UTF_8).copyOf(8)

        val buffer = ByteBuffer.allocate(16 + signature.size)
        buffer.put(empCodeBytes)
        buffer.putLong(timestamp)
        buffer.put(signature)

        val data = AdvertiseData.Builder()
            // Added as ServiceData, not ManufacturerData, because Kiosk uses record.serviceData
            .addServiceData(ParcelUuid(BLE_KIOSK_UUID), buffer.array())
            .setIncludeDeviceName(false) // Saves BLE packet bytes
            .build()

        advertiser.startAdvertising(settings, data, object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                super.onStartSuccess(settingsInEffect)
                onComplete()
            }
        })
    }
}