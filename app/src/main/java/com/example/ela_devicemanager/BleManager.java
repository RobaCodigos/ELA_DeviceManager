package com.example.ela_devicemanager;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;

public class BleManager {

    private BluetoothGatt bluetoothGatt;
    private Context appContext; // Variable para almacenar el contexto de forma segura

    public interface BleConnectionListener {
        void onConnected();
        void onDisconnected();
        void onServicesDiscovered(BluetoothGatt gatt);
    }

    private BleConnectionListener connectionListener;

    public void connect(Context context, String deviceAddress, BleConnectionListener listener) {
        this.appContext = context.getApplicationContext(); // Guardamos el contexto
        this.connectionListener = listener;

        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) return;

        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) return;

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        if (device == null) return;

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Conexión al perfil GATT del sensor
        bluetoothGatt = device.connectGatt(context, false, gattCallback);
    }

    public void disconnect(Context context) {
        if (bluetoothGatt == null) return;

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
        }
        bluetoothGatt = null;
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (connectionListener != null) {
                    connectionListener.onConnected();
                }
                // Usamos appContext en lugar de gatt.getDevice().getContext()
                if (appContext != null && ActivityCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    gatt.discoverServices();
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (connectionListener != null) {
                    connectionListener.onDisconnected();
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS && connectionListener != null) {
                connectionListener.onServicesDiscovered(gatt);
            }
        }
    };
}