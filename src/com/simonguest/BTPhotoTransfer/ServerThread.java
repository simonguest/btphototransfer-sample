package com.simonguest.BTPhotoTransfer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.util.UUID;

class ServerThread extends Thread {
    private final BluetoothServerSocket btServerSocket;
    private String TAG = "BTPHOTO/ServerThread";
    private Handler imageDisplayHandler;

    public ServerThread(BluetoothAdapter btAdapter, String name, UUID uuid, Handler imageDisplayHandler) {

        this.imageDisplayHandler = imageDisplayHandler;
        BluetoothServerSocket tmp = null;
        try {
            Log.d(TAG, "Setting up temp bt adapter with "+name+" "+uuid.toString());
            tmp = btAdapter.listenUsingInsecureRfcommWithServiceRecord(name, uuid);
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
        btServerSocket = tmp;
    }

    public void run() {
        BluetoothSocket btSocket = null;
        while (true) {
            try {
                Log.d(TAG, "Opening new server socket");
                btSocket = btServerSocket.accept();
                Log.d(TAG, "Got connection from client.  Spawning new server thread.");
                // spawn into new thread at this point
                ImageTransferThread imageTransferThread = new ImageTransferThread(btSocket, imageDisplayHandler);
                imageTransferThread.start();

            } catch (Exception e) {
                Log.d(TAG, e.toString());
                break;
            }
        }
    }

    public void cancel() {
        try {
            Log.d(TAG, "Trying to close the server socket");
            btServerSocket.close();
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
    }
}
