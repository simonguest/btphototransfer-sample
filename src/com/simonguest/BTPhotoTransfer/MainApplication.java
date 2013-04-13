package com.simonguest.BTPhotoTransfer;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.util.Set;

public class MainApplication extends Application {
    private static String TAG = "BTPHOTO/MainApplication";
    protected static BluetoothAdapter adapter;
    protected static Set<BluetoothDevice> pairedDevices;
    protected static Handler imageDisplayHandler;
    protected static Handler toastDisplayHandler;
    protected static Handler imageCaptureHandler;
    protected static ClientThread clientThread;
    protected static ServerThread serverThread;

    @Override
    public void onCreate(){
        super.onCreate();
        adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            if (adapter.isEnabled()) {
                pairedDevices = adapter.getBondedDevices();
            } else {
                Log.e(TAG, "Bluetooth is not enabled");
                Toast.makeText(this, "Bluetooth is not enabled on this device", Toast.LENGTH_LONG).show();
            }

        } else {
            Log.e(TAG, "Bluetooth is not supported on this device");
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_LONG).show();
        }
    }
}
