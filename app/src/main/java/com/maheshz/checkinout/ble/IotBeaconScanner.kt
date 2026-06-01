package com.maheshz.checkinout.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import java.util.UUID

class IotBeaconScanner(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val BLE_SERVICE_UUID = UUID.fromString("0000AAAA-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    fun startScanning(targetOrgCode: String, onDeviceFound: (String) -> Unit) {
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BLE_SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                result.scanRecord?.getManufacturerSpecificData(0x0001)?.let { data ->
                    // Parse: [0-1] magic 0xAB 0xCD, [2-17] device_id, [18-N] org_code, [N+1] version
                    if (data.size >= 19 && data[0] == 0xAB.toByte() && data[1] == 0xCD.toByte()) {
                        val deviceIdBytes = data.copyOfRange(2, 18)
                        val deviceId = String(deviceIdBytes).trimEnd('\u0000')
                        val orgCodeStr = String(data.copyOfRange(18, data.size - 1))
                        
                        if (orgCodeStr == targetOrgCode) {
                            scanner.stopScan(this)
                            onDeviceFound(deviceId)
                        }
                    }
                }
            }
        }
        scanner.startScan(listOf(filter), settings, scanCallback)
    }
}
