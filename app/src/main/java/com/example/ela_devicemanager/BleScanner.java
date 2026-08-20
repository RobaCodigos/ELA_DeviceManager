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
import androidx.core.app.ActivityCompat;

public class BleScanner {

    private final BluetoothLeScanner bluetoothLeScanner;
    private boolean isScanning = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Context appContext; // Almacenamos el contexto de forma segura

    // Tiempo límite de escaneo (10 segundos)
    private static final long SCAN_PERIOD = 10000;

    public interface BleScanListener {
        void onDeviceFound(String deviceName, String deviceAddress, int rssi);
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

        // Comprobación de permisos de escaneo requerida para Android 12+
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

            // Verificamos permisos de BLUETOOTH_CONNECT antes de leer el nombre o la MAC del dispositivo
            if (appContext != null && ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                if (result.getDevice() != null) {
                    String name = result.getDevice().getName();
                    String address = result.getDevice().getAddress();
                    int rssi = result.getRssi();

                    // Filtramos por dispositivos ELA (Blue PUCK)
                    if (name != null && (name.contains("PUCK") || name.contains("ELA"))) {
                        if (scanListener != null) {
                            scanListener.onDeviceFound(name, address, rssi);
                        }
                    }
                }
            }
        }
    };
}