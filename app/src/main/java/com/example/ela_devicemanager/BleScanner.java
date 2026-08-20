package com.example.ela_devicemanager;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("MissingPermission")
public class BleScanner {

    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner scanner;
    private boolean isScanning = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ScanCallback currentScanCallback;

    public interface BleScanListener {
        void onDeviceFound(String deviceName, String deviceAddress, int rssi, String manufacturerDataStr);
        void onScanFinished();
    }

    public BleScanner(Context context) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        } else {
            bluetoothAdapter = null;
        }
    }

    public void startScanning(Context context, final BleScanListener listener) {
        // Tiempo de escaneo agresivo: 12 segundos
        long SCAN_PERIOD_MS = 12000;

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e("BLE_SCANNER", "Bluetooth no está disponible o encendido.");
            listener.onScanFinished();
            return;
        }

        scanner = bluetoothAdapter.getBluetoothLeScanner();
        if (scanner == null) {
            listener.onScanFinished();
            return;
        }

        if (isScanning) return;

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build();

        List<ScanFilter> filters = new ArrayList<>();

        currentScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);

                ScanRecord scanRecord = result.getScanRecord();

                String deviceName = result.getDevice().getName();
                if (deviceName == null && scanRecord != null) {
                    deviceName = scanRecord.getDeviceName();
                }

                // --- FILTRO DE SENSORES ANÓNIMOS ---
                // Si el dispositivo no reporta ningún nombre o está vacío,
                // interrumpimos la ejecución y NO lo mandamos a la UI.
                if (deviceName == null || deviceName.trim().isEmpty()) {
                    return;
                }
                // -----------------------------------

                String deviceAddress = result.getDevice().getAddress();
                int rssi = result.getRssi();

                StringBuilder rawDataStr = new StringBuilder();
                if (scanRecord != null && scanRecord.getBytes() != null) {
                    for (byte b : scanRecord.getBytes()) {
                        rawDataStr.append(String.format("%02X", b));
                    }
                } else {
                    rawDataStr.append("NO_DATA");
                }

                listener.onDeviceFound(deviceName, deviceAddress, rssi, rawDataStr.toString());
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.e("BLE_SCANNER", "Fallo en el escaneo. Error Code: " + errorCode);
            }
        };

        isScanning = true;
        scanner.startScan(filters, settings, currentScanCallback);

        handler.postDelayed(() -> {
            stopScanning();
            listener.onScanFinished();
        }, SCAN_PERIOD_MS);
    }

    private void stopScanning() {
        if (isScanning && scanner != null && currentScanCallback != null) {
            try {
                scanner.stopScan(currentScanCallback);
            } catch (Exception e) {
                Log.e("BLE_SCANNER", "Error deteniendo el escaneo: " + e.getMessage());
            }
            isScanning = false;
            currentScanCallback = null;
        }
    }
}