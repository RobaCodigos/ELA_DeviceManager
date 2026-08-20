package com.example.ela_devicemanager;

import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class NfcHandler {

    // Método para leer la información básica del Tag acercándolo al móvil
    public static String readTagId(Tag tag) {
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                NdefMessage message = ndef.getNdefMessage();
                if (message != null && message.getRecords().length > 0) {
                    NdefRecord record = message.getRecords()[0];
                    byte[] payload = record.getPayload();
                    return new String(payload, StandardCharsets.UTF_8);
                }
                ndef.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Desconocido";
    }

    // Método para escribir la nueva configuración en el Blue PUCK T vía NFC
    public static boolean writeConfigurationToTag(Tag tag, String configPayload) {
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                if (!ndef.isWritable()) {
                    ndef.close();
                    return false;
                }

                NdefRecord record = NdefRecord.createMime("application/vnd.ela.config", configPayload.getBytes(StandardCharsets.UTF_8));
                NdefMessage message = new NdefMessage(new NdefRecord[]{record});

                ndef.writeNdefMessage(message);
                ndef.close();
                return true;
            }
        } catch (IOException | android.nfc.FormatException e) {
            e.printStackTrace();
        }
        return false;
    }
}