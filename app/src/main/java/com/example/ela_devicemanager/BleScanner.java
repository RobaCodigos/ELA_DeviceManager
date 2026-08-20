package com.example.ela_devicemanager;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;
import androidx.core.app.ActivityCompat;

public class BleScanner {

    private final BluetoothLeScanner bluetoothLeScanner;
    private boolean isScanning = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Context appContext;

    private static final long SCAN_PERIOD = 10000; // 10 segundos

    public interface BleScanListener {
        // CORREGIDO: Ahora devuelve 'void' en lugar de 'String'
        void onDeviceFound(String deviceName, String deviceAddress, int rssi, String manufacturerDataStr);
        void onScanFinished();
    }

    private BleScanListener scanListener;

    public BleScanner(Context context) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter != null) {
                this.bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            } else {
                this.bluetoothLeScanner = null;
            }
        } else {
            this.bluetoothLeScanner = null;
        }
    }

    public void startScanning(Context context, BleScanListener listener) {
        if (bluetoothLeScanner == null || isScanning) return;
        this.appContext = context.getApplicationContext();
        this.scanListener = listener;

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        handler.postDelayed(() -> stopScanning(context), SCAN_PERIOD);

        isScanning = true;
        bluetoothLeScanner.startScan(scanCallback);
    }

    public void stopScanning(Context context) {
        if (!isScanning || bluetoothLeScanner == null) return;

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bluetoothLeScanner.stopScan(scanCallback);
        }
        isScanning = false;
        if (scanListener != null) {
            scanListener.onScanFinished();
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            if (appContext != null && ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                if (result.getDevice() != null) {
                    String name = result.getDevice().getName();

                    // Ocultamos dispositivos sin nombre comercial
                    if (name == null || name.trim().isEmpty()) {
                        return;
                    }

                    String address = result.getDevice().getAddress();
                    int rssi = result.getRssi();

                    StringBuilder dataHex = new StringBuilder();
                    if (result.getScanRecord() != null) {
                        SparseArray<byte[]> manufacturerData = result.getScanRecord().getManufacturerSpecificData();
                        if (manufacturerData != null && manufacturerData.size() > 0) {
                            for (int i = 0; i < manufacturerData.size(); i++) {
                                int manufacturerId = manufacturerData.keyAt(i);
                                byte[] data = manufacturerData.valueAt(i);
                                dataHex.append("ID: ").append(manufacturerId).append(" [");
                                if (data != null) {
                                    for (byte b : data) {
                                        dataHex.append(String.format("%02X ", b));
                                    }
                                }
                                dataHex.append("]");
                            }
                        } else {
                            dataHex.append("Sin datos de fabricante");
                        }
                    }

                    if (scanListener != null) {
                        scanListener.onDeviceFound(name, address, rssi, dataHex.toString());
                    }
                }
            }
        }
    };
}