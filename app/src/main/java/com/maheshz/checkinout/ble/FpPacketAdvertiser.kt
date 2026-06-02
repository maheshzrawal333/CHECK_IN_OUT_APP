package com.maheshz.checkinout.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import java.util.UUID

class FpPacketAdvertiser(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val BLE_RESPONSE_UUID = UUID.fromString("0000BBBB-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    fun startAdvertising(employeeCode: String, timestamp: Long, signature: ByteArray, onComplete: () -> Unit) {
        val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser ?: return

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .setTimeout(10000)
            .build()

        // 🌟 ENTERPRISE FIX: Increased packet size to 100 to safely hold a 72-byte DER ECDSA signature
        val packet = ByteArray(100)
        packet[0] = 0xCD.toByte()
        packet[1] = 0xAB.toByte()

        val empBytes = employeeCode.padEnd(16, ' ').toByteArray(Charsets.UTF_8).copyOf(16)
        System.arraycopy(empBytes, 0, packet, 2, 16)

        val t = timestamp.toInt()
        packet[18] = (t shr 24).toByte()
        packet[19] = (t shr 16).toByte()
        packet[20] = (t shr 8).toByte()
        packet[21] = t.toByte()

        // Safely copy the entire signature without truncation
        System.arraycopy(signature, 0, packet, 22, signature.size)

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(BLE_RESPONSE_UUID))
            .addManufacturerData(0x0002, packet)
            .build()

        advertiser.startAdvertising(settings, data, object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                super.onStartSuccess(settingsInEffect)
                onComplete()
            }
        })
    }
}