package com.simonguest.BTPhotoTransfer;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import java.util.Set;
import java.util.UUID;

public class MainApplication extends Application {

    protected static final String NAME = "BTPHOTO";
    protected static final String UUID_STRING = "00001101-0000-1000-8000-00805F9B34AC";
    public static final int PICTURE_RESULT = 1234;
    public static BluetoothAdapter btAdapter;
    public static Set<BluetoothDevice> pairedDevices;
    public static Handler imageDisplayHandler;
    public static Handler toastDisplayHandler;
    public static Handler imageCaptureHandler;
    public static ClientThread clientThread;
    public static ServerThread serverThread;
    public static String TAG = "BTPHOTO/MainApplication";

    @Override
    public void onCreate(){
        super.onCreate();
        // Setup the Bluetooth Connection
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            if (btAdapter.isEnabled()) {
                pairedDevices = btAdapter.getBondedDevices();
            } else {
                Log.d(TAG, "Bluetooth is not enabled");
                Toast.makeText(this, "Bluetooth is not enabled on this device", Toast.LENGTH_LONG).show();
            }

        } else {
            Log.d(TAG, "Bluetooth is not supported on this device");
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_LONG).show();
        }
    }
}
