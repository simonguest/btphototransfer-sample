package com.simonguest.BTPhotoTransfer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

class ServerThread extends Thread {
    private final String TAG = "BTPHOTO/ServerThread";
    private final BluetoothServerSocket serverSocket;
    private Handler imageDisplayHandler;

    public ServerThread(BluetoothAdapter adapter, String name, UUID uuid, Handler imageDisplayHandler) {
        this.imageDisplayHandler = imageDisplayHandler;
        BluetoothServerSocket tempSocket = null;
        try {
            tempSocket = adapter.listenUsingInsecureRfcommWithServiceRecord(name, uuid);
        } catch (IOException ioe) {
            Log.e(TAG, ioe.toString());
        }
        serverSocket = tempSocket;
    }

    public void run() {
        BluetoothSocket socket = null;
        if (serverSocket == null)
        {
            // something went wrong with initialization
            Log.d(TAG, "Server socket is null - something went wrong with Bluetooth stack initialization?");
            return;
        }
        while (true) {
            try {
                Log.v(TAG, "Opening new server socket");
                socket = serverSocket.accept();

                try {
                    Log.v(TAG, "Got connection from client.  Spawning new image transfer thread.");
                    ImageTransferThread imageTransferThread = new ImageTransferThread(socket, imageDisplayHandler);
                    imageTransferThread.start();
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }

            } catch (IOException ioe) {
                // Suppress the exception (socket was likely closed by activity change)
                Log.v(TAG, "Server socket was closed - likely due to activity change");
                break;
            }
        }
    }

    public void cancel() {
        try {
            Log.v(TAG, "Trying to close the server socket");
            serverSocket.close();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }
}
