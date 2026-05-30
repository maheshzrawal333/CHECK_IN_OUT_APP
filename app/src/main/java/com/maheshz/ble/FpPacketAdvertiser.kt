package com.maheshz.ble

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
    fun startAdvertising(employeeCode: String, signature: ByteArray, onComplete: () -> Unit) {
        val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser ?: return
        
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .setTimeout(10000)
            .build()
            
        // Payload size limit might be an issue. BLE manufacturer data max size is ~26 bytes normally, or extended advertising.
        // For standard BLE, 64-byte signature + 16-byte id + 2-byte magic + 1-byte ver = 83 bytes.
        // Requires BLE 5.0 Extended Advertising. We assume standard supports it or we segment, but prompt says:
        // [0-1] magic, [2-17] code, [18-81] sig, [82] ver.
        val packet = ByteArray(83)
        packet[0] = 0xCD.toByte()
        packet[1] = 0xAB.toByte()
        
        val empBytes = employeeCode.toByteArray()
        System.arraycopy(empBytes, 0, packet, 2, minOf(empBytes.size, 16))
        
        System.arraycopy(signature, 0, packet, 18, 64)
        packet[82] = 0x01
        
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
