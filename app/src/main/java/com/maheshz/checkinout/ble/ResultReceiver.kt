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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.coroutines.resume

data class BleResult(val success: Boolean, val message: String)

class ResultReceiver(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    
    private val BLE_SUCCESS_UUID = UUID.fromString("0000CCCC-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    suspend fun waitForResult(employeeCode: String): BleResult? {
        return withTimeoutOrNull(10_000) {
            suspendCancellableCoroutine { continuation ->
                val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return@suspendCancellableCoroutine
                val filter = ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(BLE_SUCCESS_UUID))
                    .build()
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

                val scanCallback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        super.onScanResult(callbackType, result)
                        result.scanRecord?.getManufacturerSpecificData(0x0003)?.let { data ->
                            if (data.size >= 18) {
                                val successMark = data[0]
                                val empBytes = data.copyOfRange(1, 17)
                                val empStr = String(empBytes).trimEnd('\u0000')
                                if (empStr == employeeCode) {
                                    scanner.stopScan(this)
                                    val message = String(data.copyOfRange(17, data.size))
                                    continuation.resume(BleResult(successMark == 0x01.toByte(), message))
                                }
                            }
                        }
                    }
                }

                scanner.startScan(listOf(filter), settings, scanCallback)
                
                continuation.invokeOnCancellation {
                    scanner.stopScan(scanCallback)
                }
            }
        }
    }
}
