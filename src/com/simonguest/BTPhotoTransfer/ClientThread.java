package com.simonguest.BTPhotoTransfer;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ClientThread extends Thread {
    private final String TAG = "BTPHOTO/ClientThread";
    private final BluetoothSocket socket;
    private Handler toastDisplayHandler;
    private Handler imageCaptureHandler;

    public ClientThread(BluetoothDevice device, UUID uuid, Handler toastDisplayHandler, Handler imageCaptureHandler) {
        BluetoothSocket tempSocket = null;
        this.toastDisplayHandler = toastDisplayHandler;
        this.imageCaptureHandler = imageCaptureHandler;

        try {
            tempSocket = device.createRfcommSocketToServiceRecord(uuid);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        this.socket = tempSocket;
    }

    public void run() {
        try {
            Log.v(TAG, "Opening client socket");
            socket.connect();
            Log.v(TAG, "Connection established");

            // Now that we have a socket, callback and request an image from the UI thread
            imageCaptureHandler.sendEmptyMessage(0);

        } catch (IOException ioe) {
            // Couldn't connect to the server - let the user know
            Message message = new Message();
            message.obj = "Could not connect to the other device.  Is the application running and/or the device in range?";
            toastDisplayHandler.sendMessage(message);
            Log.e(TAG, ioe.toString());
            try {
                socket.close();
            } catch (IOException ce) {
                Log.e(TAG, "Socket close exception: " + ce.toString());
            }
        }
    }

    public void sendImage(Bitmap image) {
        try {
            OutputStream outputStream = socket.getOutputStream();
            ByteArrayOutputStream compressedImageStream = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, Constants.IMAGE_QUALITY, compressedImageStream);
            byte[] compressedImage = compressedImageStream.toByteArray();
            Log.v(TAG, "Compressed image size: " + compressedImage.length);

            // Send the header control first
            outputStream.write(Constants.HEADER_MSB);
            outputStream.write(Constants.HEADER_LSB);

            // write image size
            outputStream.write(Utils.intToByteArray(compressedImage.length));

            // write image digest
            byte[] imageDigest = Utils.getImageDigest(compressedImage);
            outputStream.write(imageDigest);

            // now write the image
            outputStream.write(compressedImage);
            outputStream.flush();

            Log.v(TAG, "Image sent.  Waiting for image digest as confirmation");
            InputStream inputStream = socket.getInputStream();
            byte[] incomingImageDigest = new byte[16];
            int incomingImageIndex = 0;

            try {
                while (true) {
                    byte[] header = new byte[1];
                    inputStream.read(header, 0, 1);
                    incomingImageDigest[incomingImageIndex++] = header[0];
                    if (incomingImageIndex == 16) {
                        if (Utils.imageDigestMatch(compressedImage, incomingImageDigest)) {
                            Log.v(TAG, "Digest matched OK.  Image was received OK.");
                            // Pass back message to the caller
                            Message message = new Message();
                            message.obj = "Image was received OK by host.";
                            toastDisplayHandler.dispatchMessage(message);
                        } else {
                            Log.e(TAG, "Digest did not match.  Might want to resend.");
                        }

                        break;
                    }
                }
            } catch (Exception ex) {
                Log.e(TAG, ex.toString());
            }

            Log.v(TAG, "Closing the client socket.");
            socket.close();

        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    public void cancel() {
        try {
            if (socket.isConnected()) {
                socket.close();
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }
}