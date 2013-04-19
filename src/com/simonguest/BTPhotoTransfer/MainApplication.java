package com.simonguest.BTPhotoTransfer;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.util.Log;
import com.simonguest.btxfr.ClientThread;
import com.simonguest.btxfr.ProgressData;
import com.simonguest.btxfr.ServerThread;

import java.util.Set;

public class MainApplication extends Application {
    private static String TAG = "BTPHOTO/MainApplication";
    protected static BluetoothAdapter adapter;
    protected static Set<BluetoothDevice> pairedDevices;
    protected static Handler clientHandler;
    protected static Handler serverHandler;
    protected static ClientThread clientThread;
    protected static ServerThread serverThread;
    protected static ProgressData progressData = new ProgressData();

    protected static final String TEMP_IMAGE_FILE_NAME = "btimage.jpg";
    protected static final int PICTURE_RESULT_CODE = 1234;
    protected static final int IMAGE_QUALITY = 100;


    @Override
    public void onCreate() {
        super.onCreate();
        adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            if (adapter.isEnabled()) {
                pairedDevices = adapter.getBondedDevices();
            } else {
                Log.e(TAG, "Bluetooth is not enabled");
            }
        } else {
            Log.e(TAG, "Bluetooth is not supported on this device");
        }
    }
}
